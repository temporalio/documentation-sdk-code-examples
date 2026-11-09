# frozen_string_literal: true

require 'minitest/autorun'
require 'securerandom'
require 'temporalio/testing'
require 'temporalio/worker'
require_relative '../my_workflow'

class ActivityExecutionTest < Minitest::Test
  def test_execute_activity
    Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ) do |env|
      client = env.client
      task_queue = "activity-execution-#{SecureRandom.uuid}"
      Temporalio::Worker.new(client:, task_queue:, workflows: [MyWorkflow], activities: [MyActivity]).run do
        # The Workflow runs under its custom name.
        handle = client.start_workflow('MyDifferentWorkflowName', 'Temporal', id: "wf-#{SecureRandom.uuid}", task_queue:)

        # The Activity result is returned from execute_activity.
        assert_equal 'Hello, Temporal!', handle.result

        # The call produced ActivityTaskScheduled, ActivityTaskStarted, and ActivityTaskCompleted.
        events = handle.fetch_history_events.to_a
        types = events.map(&:event_type)
        %i[EVENT_TYPE_ACTIVITY_TASK_SCHEDULED EVENT_TYPE_ACTIVITY_TASK_STARTED
           EVENT_TYPE_ACTIVITY_TASK_COMPLETED].each { |t| assert_includes types, t }

        scheduled = events.find(&:activity_task_scheduled_event_attributes).activity_task_scheduled_event_attributes
        assert_equal 'MyActivity', scheduled.activity_type.name
        assert_equal 100, scheduled.start_to_close_timeout.seconds
      end
    end
  end
end
