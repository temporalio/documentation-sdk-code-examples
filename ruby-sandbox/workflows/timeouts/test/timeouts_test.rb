# frozen_string_literal: true

require 'minitest/autorun'
require 'temporalio/testing'
require 'temporalio/worker'
require_relative '../client'

class TimeoutsTest < Minitest::Test
  def self.env
    @env ||= Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ).tap { |env| Minitest.after_run { env.shutdown } }
  end

  def client
    self.class.env.client
  end

  # The client functions use the page's fixed Workflow ID and Task Queue, so read the options back from the
  # WorkflowExecutionStarted event of the latest run.
  def started_event
    client.workflow_handle('my-workflow-id').fetch_history_events.first.workflow_execution_started_event_attributes
  end

  def with_worker(&)
    Temporalio::Worker.new(client:, task_queue: 'my-task-queue', workflows: [MyWorkflow]).run(&)
  end

  def test_execution_timeout
    with_worker do
      assert_equal 'Received some-input', execute_with_timeout(client)
      assert_equal 300, started_event.workflow_execution_timeout.seconds
    end
  end

  def test_retry_policy
    with_worker do
      assert_equal 'Received some-input', execute_with_retry_policy(client)
      assert_equal 10, started_event.retry_policy.maximum_interval.seconds
    end
  end
end
