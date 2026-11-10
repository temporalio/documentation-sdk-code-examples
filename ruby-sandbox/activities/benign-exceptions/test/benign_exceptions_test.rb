# frozen_string_literal: true

require 'logger'
require 'minitest/autorun'
require 'securerandom'
require 'stringio'
require 'temporalio/testing'
require 'temporalio/worker'
require 'temporalio/workflow'
require_relative '../my_activity'

class CallMyActivityWorkflow < Temporalio::Workflow::Definition
  def execute
    Temporalio::Workflow.execute_activity(
      MyActivity, start_to_close_timeout: 10, retry_policy: Temporalio::RetryPolicy.new(max_attempts: 1)
    )
  end
end

class BenignExceptionsTest < Minitest::Test
  def test_benign_failure_logs_at_debug
    Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ) do |env|
      log = StringIO.new
      task_queue = "benign-#{SecureRandom.uuid}"
      Temporalio::Worker.new(
        client: env.client, task_queue:, workflows: [CallMyActivityWorkflow], activities: [MyActivity],
        logger: Logger.new(log, level: Logger::DEBUG)
      ).run do
        err = assert_raises(Temporalio::Error::WorkflowFailedError) do
          env.client.execute_workflow(CallMyActivityWorkflow, id: "wf-#{SecureRandom.uuid}", task_queue:)
        end
        # The Workflow still sees the error, with its category.
        app_err = err.cause.cause
        assert_equal 'Service not ready', app_err.message
        assert_equal Temporalio::Error::ApplicationError::Category::BENIGN, app_err.category
      end
      failure_lines = log.string.lines.grep(/Completing activity as failed/)
      refute_empty failure_lines
      failure_lines.each { |l| assert_match(/\AD, /, l) }
    end
  end
end
