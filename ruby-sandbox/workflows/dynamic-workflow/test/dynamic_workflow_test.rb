# frozen_string_literal: true

require 'minitest/autorun'
require 'securerandom'
require 'temporalio/testing'
require 'temporalio/worker'
require_relative '../my_dynamic_workflow'

# A registered Workflow, to show it takes precedence over the Dynamic Workflow.
class StaticWorkflow < Temporalio::Workflow::Definition
  def execute(_name)
    'static'
  end
end

class DynamicWorkflowTest < Minitest::Test
  def test_dynamic_workflow
    Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ) do |env|
      client = env.client
      task_queue = "dynamic-workflow-#{SecureRandom.uuid}"
      Temporalio::Worker.new(
        client:, task_queue:, workflows: [MyDynamicWorkflow, StaticWorkflow], activities: [MyActivity]
      ).run do
        run = ->(type, *args) { client.execute_workflow(type, *args, id: "wf-#{SecureRandom.uuid}", task_queue:) }

        # Any unregistered Workflow Type runs the Dynamic Workflow.
        assert_equal 'Hello, Temporal!', run.call('SomeUnregisteredWorkflow', 'Temporal')
        assert_equal 'Hello, Ruby!', run.call('AnotherUnregisteredWorkflow', 'Ruby')
        assert_equal 'static', run.call('StaticWorkflow', 'Temporal')

        err = assert_raises(Temporalio::Error::WorkflowFailedError) { run.call('SomeUnregisteredWorkflow', 'a', 'b') }
        assert_equal 'One arg expected', err.cause.message
      end
    end
  end
end
