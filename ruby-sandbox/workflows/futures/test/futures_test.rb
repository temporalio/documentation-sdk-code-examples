# frozen_string_literal: true

require 'minitest/autorun'
require 'securerandom'
require 'temporalio/testing'
require 'temporalio/worker'
require_relative '../all_of_workflow'
require_relative '../any_of_workflow'

class FuturesTest < Minitest::Test
  def self.env
    @env ||= Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ).tap { |env| Minitest.after_run { env.shutdown } }
  end

  def run_workflow(workflow)
    client = self.class.env.client
    task_queue = "futures-#{SecureRandom.uuid}"
    Temporalio::Worker.new(
      client:, task_queue:, workflows: [workflow],
      activities: [MyActivity1, MyActivity2, MyActivity3, MyActivity]
    ).run do
      handle = client.start_workflow(workflow, id: "wf-#{SecureRandom.uuid}", task_queue:)
      [handle.result, handle.fetch_history_events]
    end
  end

  def test_all_of
    result, events = run_workflow(AllOfWorkflow)
    assert_equal %w[one two three], result

    # All three Activities are scheduled in the same Workflow Task, so they run at once.
    scheduled = events.filter_map(&:activity_task_scheduled_event_attributes)
    assert_equal 3, scheduled.size
    assert_equal 1, scheduled.map(&:workflow_task_completed_event_id).uniq.size
  end

  def test_any_of_returns_first_activity_result
    result, events = run_workflow(AnyOfWorkflow)
    assert_match(/\Aresult for my-arg-[0-4]\z/, result)
    assert_equal 5, events.count(&:activity_task_scheduled_event_attributes)
    assert(events.any?(&:timer_started_event_attributes))
  end
end
