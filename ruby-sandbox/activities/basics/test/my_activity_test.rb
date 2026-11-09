# frozen_string_literal: true

require 'minitest/autorun'
require 'securerandom'
require 'temporalio/testing'
require 'temporalio/worker'
require 'temporalio/workflow'
require_relative '../my_activity'

# Calls MyActivity with a single hash argument, as the page recommends.
class CallMyActivityWorkflow < Temporalio::Workflow::Definition
  def execute(input)
    Temporalio::Workflow.execute_activity(MyActivity, input, start_to_close_timeout: 10)
  end
end

class MyActivityTest < Minitest::Test
  def test_default_name_is_unqualified_class_name
    assert_equal 'MyActivity', Temporalio::Activity::Definition::Info.from_activity(MyActivity).name
  end

  def test_runs_in_workflow
    Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ) do |env|
      task_queue = "activity-basics-#{SecureRandom.uuid}"
      Temporalio::Worker.new(
        client: env.client, task_queue:, workflows: [CallMyActivityWorkflow], activities: [MyActivity]
      ).run do
        # Hash keys arrive in the Activity as strings after JSON conversion.
        result = env.client.execute_workflow(
          CallMyActivityWorkflow, { greeting: 'Hello', name: 'Temporal' },
          id: "wf-#{SecureRandom.uuid}", task_queue:
        )
        assert_equal 'Hello, Temporal!', result
      end
    end
  end
end
