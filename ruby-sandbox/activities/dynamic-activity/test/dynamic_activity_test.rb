# frozen_string_literal: true

require 'minitest/autorun'
require 'securerandom'
require 'temporalio/testing'
require 'temporalio/worker'
require 'temporalio/workflow'
require_relative '../my_dynamic_activity'

# A registered Activity, to show it takes precedence over the Dynamic Activity.
class StaticActivity < Temporalio::Activity::Definition
  def execute(_input)
    'static'
  end
end

# Calls an Activity Type by name with the given arguments.
class CallActivityByNameWorkflow < Temporalio::Workflow::Definition
  def execute(activity_type, args)
    Temporalio::Workflow.execute_activity(
      activity_type, *args,
      start_to_close_timeout: 10, retry_policy: Temporalio::RetryPolicy.new(max_attempts: 1)
    )
  end
end

class DynamicActivityTest < Minitest::Test
  def test_dynamic_activity
    Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ) do |env|
      client = env.client
      task_queue = "dynamic-activity-#{SecureRandom.uuid}"
      Temporalio::Worker.new(
        client:, task_queue:, workflows: [CallActivityByNameWorkflow], activities: [MyDynamicActivity, StaticActivity]
      ).run do
        run = lambda do |type, *args|
          client.execute_workflow(CallActivityByNameWorkflow, type, args, id: "wf-#{SecureRandom.uuid}", task_queue:)
        end
        input = { greeting: 'Hello', name: 'Temporal' }

        # Any unregistered Activity Type runs the Dynamic Activity.
        assert_equal 'Hello, Temporal!', run.call('SomeUnregisteredActivity', input)
        assert_equal 'Hello, Temporal!', run.call('AnotherUnregisteredActivity', input)
        assert_equal 'static', run.call('StaticActivity', input)

        err = assert_raises(Temporalio::Error::WorkflowFailedError) { run.call('SomeUnregisteredActivity', input, input) }
        assert_equal 'One arg expected', err.cause.cause.message
      end
    end
  end

  def test_only_one_dynamic_activity_per_worker
    another = Class.new(Temporalio::Activity::Definition) do
      activity_dynamic
      def execute(*) = nil
    end
    Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ) do |env|
      assert_raises(ArgumentError) do
        Temporalio::Worker.new(client: env.client, task_queue: 'unused', activities: [MyDynamicActivity, another])
      end
    end
  end
end
