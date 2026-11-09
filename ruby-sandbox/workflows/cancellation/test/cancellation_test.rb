# frozen_string_literal: true

require 'minitest/autorun'
require 'securerandom'
require 'temporalio/testing'
require 'temporalio/worker'
require_relative '../handle_cancellation_workflow'
require_relative '../cancel_activity_workflow'
require_relative '../client'

# Runs MyActivity with a Heartbeat Timeout and waits for the Activity to handle cancellation, so the test can see the
# error the Activity raises.
class WaitForActivityCancellationWorkflow < Temporalio::Workflow::Definition
  def execute
    Temporalio::Workflow.execute_activity(
      MyActivity,
      start_to_close_timeout: 100,
      heartbeat_timeout: 5,
      cancellation_type: Temporalio::Workflow::ActivityCancellationType::WAIT_CANCELLATION_COMPLETED
    )
  rescue Temporalio::Error::ActivityError => e
    e.cause.message
  end
end

class CancellationTest < Minitest::Test
  Status = Temporalio::Client::WorkflowExecutionStatus

  def self.env
    @env ||= Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ).tap { |env| Minitest.after_run { env.shutdown } }
  end

  def client
    self.class.env.client
  end

  def with_worker(client, workflow, &)
    @task_queue = "cancellation-#{SecureRandom.uuid}"
    Temporalio::Worker.new(
      client:, task_queue: @task_queue, workflows: [workflow], activities: [MyActivity, MyCleanupActivity]
    ).run(&)
  end

  def start(client, workflow, id: "wf-#{SecureRandom.uuid}")
    client.start_workflow(workflow, id:, task_queue: @task_queue)
  end

  # ActivityTaskStarted isn't written to the Event History until the Activity closes, so check pending Activities.
  def wait_for_running_activity(handle)
    deadline = Time.now + 10
    loop do
      pending = handle.describe.raw_description.pending_activities
      break if pending.any? { |a| a.state == :PENDING_ACTIVITY_STATE_STARTED }
      raise 'Activity did not start' if Time.now > deadline

      sleep 0.1
    end
  end

  def test_cancel_runs_cleanup_activity
    with_worker(client, HandleCancellation::MyWorkflow) do
      handle = start(client, HandleCancellation::MyWorkflow, id: 'my-workflow-id')
      wait_for_running_activity(handle)
      cancel_workflow(client)

      assert_raises(Temporalio::Error::WorkflowFailedError) { handle.result }
      assert_equal Status::CANCELED, handle.describe.status
      scheduled = handle.fetch_history_events.filter_map(&:activity_task_scheduled_event_attributes)
      assert_equal %w[MyActivity MyCleanupActivity], scheduled.map { |a| a.activity_type.name }
    end
  end

  def test_activity_raises_on_cancellation
    with_worker(client, WaitForActivityCancellationWorkflow) do
      handle = start(client, WaitForActivityCancellationWorkflow)
      wait_for_running_activity(handle)
      handle.cancel
      # The Activity gets the cancellation on its next Heartbeat and raises.
      assert_equal 'Canceled!', handle.result
    end
  end

  def test_terminate
    with_worker(client, HandleCancellation::MyWorkflow) do
      handle = start(client, HandleCancellation::MyWorkflow, id: 'my-workflow-id')
      wait_for_running_activity(handle)
      terminate_workflow(client)

      assert_equal Status::TERMINATED, handle.describe.status
      scheduled = handle.fetch_history_events.filter_map(&:activity_task_scheduled_event_attributes)
      assert_equal %w[MyActivity], scheduled.map { |a| a.activity_type.name }
    end
  end

  # The 5-minute Timer isn't skipped: the time-skipping test server doesn't advance time while MyActivity runs. This
  # cancels the Workflow during the Timer instead, which shows the linked cancellation passes the Workflow's
  # cancellation on to the Activity.
  def test_linked_cancellation_inherits_workflow_cancellation
    with_worker(client, CancelActivity::MyWorkflow) do
      handle = start(client, CancelActivity::MyWorkflow)
      wait_for_running_activity(handle)
      handle.cancel

      assert_raises(Temporalio::Error::WorkflowFailedError) { handle.result }
      assert_equal Status::CANCELED, handle.describe.status
      events = handle.fetch_history_events
      timer = events.filter_map(&:timer_started_event_attributes).first
      assert_equal 300, timer.start_to_fire_timeout.seconds
      assert(events.any?(&:activity_task_cancel_requested_event_attributes))
    end
  end
end
