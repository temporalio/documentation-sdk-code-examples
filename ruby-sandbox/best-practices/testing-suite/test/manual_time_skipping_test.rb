# frozen_string_literal: true

require 'minitest/autorun'
require 'securerandom'
require 'temporalio/testing'
require 'temporalio/worker'
require_relative '../signal_workflow'

# Checks that env.sleep, not handle.result, skips past the 45-second timeout.
class ManualTimeSkippingTest < Minitest::Test
  def test_env_sleep_fires_the_timeout
    Temporalio::Testing::WorkflowEnvironment.start_time_skipping do |env|
      worker = Temporalio::Worker.new(client: env.client, task_queue: "tq-#{SecureRandom.uuid}", workflows: [SignalWorkflow])
      worker.run do
        handle = env.client.start_workflow(SignalWorkflow, id: "wf-#{SecureRandom.uuid}",
                                                           task_queue: worker.task_queue)
        env.sleep(40)
        sleep 0.5
        assert_equal Temporalio::Client::WorkflowExecutionStatus::RUNNING, handle.describe.status
        env.sleep(10)
        sleep 0.2 until handle.describe.status != Temporalio::Client::WorkflowExecutionStatus::RUNNING
        assert_equal Temporalio::Client::WorkflowExecutionStatus::COMPLETED, handle.describe.status
        assert_equal 'got timeout', handle.result
      end
    end
  end
end
