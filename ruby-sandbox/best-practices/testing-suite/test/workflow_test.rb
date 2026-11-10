# frozen_string_literal: true

require 'minitest/autorun'
require 'securerandom'
require 'temporalio/testing'
require 'temporalio/worker'
require_relative '../simple_workflow'
require_relative '../wait_a_day_workflow'
require_relative '../signal_workflow'

# The page's Workflow tests, runnable as written.
class WorkflowTest < Minitest::Test
  # @@@SNIPSTART ruby-testing-simple-workflow-test
  def test_simple_workflow
    # Start local server that is stopped when block is done
    Temporalio::Testing::WorkflowEnvironment.start_local do |env|
      # Start worker that is stopped when block is done
      worker = Temporalio::Worker.new(
        client: env.client,
        task_queue: "tq-#{SecureRandom.uuid}",
        workflows: [SimpleWorkflow]
      )
      worker.run do
        # Execute workflow and check result
        result = env.client.execute_workflow(
          SimpleWorkflow, 'some-name',
          id: "wf-#{SecureRandom.uuid}", task_queue: worker.task_queue
        )
        assert_equal 'Hello, some-name!', result
      end
    end
  end
  # @@@SNIPEND

  # @@@SNIPSTART ruby-testing-wait-a-day-workflow-test
  def test_wait_a_day_workflow
    # Start time-skipping test server that is stopped when block is done
    Temporalio::Testing::WorkflowEnvironment.start_time_skipping do |env|
      # Start worker that is stopped when block is done
      worker = Temporalio::Worker.new(
        client: env.client,
        task_queue: "tq-#{SecureRandom.uuid}",
        workflows: [WaitADayWorkflow]
      )
      worker.run do
        # Execute workflow and check result
        result = env.client.execute_workflow(
          WaitADayWorkflow,
          id: "wf-#{SecureRandom.uuid}", task_queue: worker.task_queue
        )
        assert_equal 'all done', result
      end
    end
  end
  # @@@SNIPEND

  # @@@SNIPSTART ruby-testing-signal-workflow-test
  def test_signal_workflow
    Temporalio::Testing::WorkflowEnvironment.start_time_skipping do |env|
      worker = Temporalio::Worker.new(
        client: env.client,
        task_queue: "tq-#{SecureRandom.uuid}",
        workflows: [SignalWorkflow]
      )
      worker.run do
        handle = env.client.start_workflow(
          SignalWorkflow,
          id: "wf-#{SecureRandom.uuid}", task_queue: worker.task_queue
        )
        handle.signal(SignalWorkflow.some_signal)
        assert_equal 'got signal', handle.result
      end
    end
  end
  # @@@SNIPEND

  # @@@SNIPSTART ruby-testing-signal-workflow-timeout-test
  def test_signal_workflow_timeout
    Temporalio::Testing::WorkflowEnvironment.start_time_skipping do |env|
      worker = Temporalio::Worker.new(
        client: env.client,
        task_queue: "tq-#{SecureRandom.uuid}",
        workflows: [SignalWorkflow]
      )
      worker.run do
        handle = env.client.start_workflow(
          SignalWorkflow,
          id: "wf-#{SecureRandom.uuid}", task_queue: worker.task_queue
        )
        # Advance 50 seconds
        env.sleep(50)
        assert_equal 'got timeout', handle.result
      end
    end
  end
  # @@@SNIPEND
end
