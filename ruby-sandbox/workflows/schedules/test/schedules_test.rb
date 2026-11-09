# frozen_string_literal: true

require 'minitest/autorun'
require 'temporalio/testing'
require 'temporalio/worker'
require_relative '../schedules'

class SchedulesTest < Minitest::Test
  def self.env
    @env ||= Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ).tap { |env| Minitest.after_run { env.shutdown } }
  end

  def client
    self.class.env.client
  end

  def eventually(timeout: 15)
    deadline = Time.now + timeout
    loop do
      result = yield
      return result if result
      raise 'Condition not met in time' if Time.now > deadline

      sleep 0.2
    end
  end

  # One test so the steps run in order against the page's fixed Schedule ID.
  def test_schedule_lifecycle
    Temporalio::Worker.new(client:, task_queue: 'my-task-queue', workflows: [MyWorkflow, MyNewWorkflow]).run do
      handle = create_schedule(client)
      assert_equal 'my-schedule-id', handle.id
      desc = nil
      assert_output(/Schedule info:/) { desc = describe_schedule(client) }
      assert_equal 'MyWorkflow', desc.schedule.action.workflow
      assert_equal 5 * 24 * 60 * 60, desc.schedule.spec.intervals.first.every

      backfill_schedule(client)

      # List is eventually consistent, so wait for the new Schedule to show up.
      eventually { client.list_schedules.any? { |s| s.id == 'my-schedule-id' } }
      assert_output(/Schedule info:/) { list_schedules(client) }

      trigger_schedule(client)
      eventually { handle.describe.info.num_actions >= 1 }
      run = handle.describe.info.recent_actions.last.action
      assert_equal 'Received some-input', client.workflow_handle(run.workflow_id, run_id: run.first_execution_run_id).result

      pause_schedule(client)
      state = handle.describe.schedule.state
      assert state.paused
      assert_equal 'Pausing the schedule for now', state.note

      update_schedule(client)
      updated = handle.describe.schedule
      assert_equal 'MyNewWorkflow', updated.action.workflow
      assert_equal ['some-new-input'], updated.action.args
      assert updated.state.paused, 'update keeps the rest of the Schedule'

      delete_schedule(client)
      err = assert_raises(Temporalio::Error::RPCError) { handle.describe }
      assert_equal Temporalio::Error::RPCError::Code::NOT_FOUND, err.code
    end
  end

  def test_start_delay
    handle = start_with_delay(client)
    started = handle.fetch_history_events.first.workflow_execution_started_event_attributes
    assert_equal 3 * 60 * 60, started.first_workflow_task_backoff.seconds
    handle.terminate
  end
end
