# frozen_string_literal: true

require 'minitest/autorun'
require 'securerandom'
require 'temporalio/testing'
require 'temporalio/worker'
require 'temporalio/workflow'
require_relative '../my_activity'
require_relative '../complete_activity'

class AsyncActivityWorkflow < Temporalio::Workflow::Definition
  def execute
    Temporalio::Workflow.execute_activity(MyAsyncActivity, start_to_close_timeout: 30)
  end
end

class AsyncActivityTest < Minitest::Test
  def test_complete_from_outside_the_activity
    Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ) do |env|
      client = env.client
      tokens = Queue.new
      task_queue = "async-activity-#{SecureRandom.uuid}"
      Temporalio::Worker.new(
        client:, task_queue:, workflows: [AsyncActivityWorkflow], activities: [MyAsyncActivity.new(tokens)]
      ).run do
        handle = client.start_workflow(AsyncActivityWorkflow, id: "wf-#{SecureRandom.uuid}", task_queue:)
        token = tokens.pop(timeout: 10)
        refute_nil token

        # The Activity returned, but its execution stays open until completed.
        pending = handle.describe.raw_description.pending_activities
        assert_equal 1, pending.size

        # The handle also heartbeats.
        client.async_activity_handle(token).heartbeat

        complete_activity(client, token)
        assert_equal 'completion value', handle.result
      end
    end
  end
end
