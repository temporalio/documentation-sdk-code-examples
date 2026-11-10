# frozen_string_literal: true

require 'minitest/autorun'
require 'securerandom'
require 'temporalio/testing'
require 'temporalio/worker'
require 'temporalio/workflow'

# Each Activity file defines SomethingThatFails, so each loads into its own module.
# The Activity Type is still SomethingThatFails.
module Examples
  def self.load(module_name, file)
    path = File.expand_path("../#{file}", __dir__)
    mod = const_set(module_name, Module.new)
    mod.module_eval(File.read(path), path)
    mod.const_get(:SomethingThatFails)
  end
  CUSTOM_ERROR = load(:CustomError, 'custom_error_activity.rb')
  APPLICATION_ERROR = load(:ApplicationError, 'application_error_activity.rb')
  NON_RETRYABLE = load(:NonRetryable, 'non_retryable_activity.rb')
end

# SagaWorkflow calls SomethingThatFails; use the non-retryable one.
SomethingThatFails = Examples::NON_RETRYABLE
require_relative '../saga_workflow'

# Runs SomethingThatFails once and returns the error the Workflow receives.
class CaptureErrorWorkflow < Temporalio::Workflow::Definition
  def execute
    Temporalio::Workflow.execute_activity(
      'SomethingThatFails', 'details',
      start_to_close_timeout: 10, retry_policy: Temporalio::RetryPolicy.new(max_attempts: 1)
    )
  rescue Temporalio::Error::ActivityError => e
    { 'type' => e.cause.type, 'message' => e.cause.message, 'non_retryable' => e.cause.non_retryable }
  end
end

class ErrorHandlingTest < Minitest::Test
  def with_worker(workflows, activity)
    Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ) do |env|
      task_queue = "errors-#{SecureRandom.uuid}"
      Temporalio::Worker.new(client: env.client, task_queue:, workflows:, activities: [activity]).run do
        yield ->(wf) { env.client.start_workflow(wf, 'details', id: "wf-#{SecureRandom.uuid}", task_queue:) }
      end
    end
  end

  def captured_error(activity)
    with_worker([CaptureErrorWorkflow], activity) { |start| start.call(CaptureErrorWorkflow).result }
  end

  def test_custom_exception_becomes_application_error
    expected = { 'type' => 'MyError', 'message' => 'Simulated failure', 'non_retryable' => false }
    assert_equal expected, captured_error(Examples::CUSTOM_ERROR)
    assert_equal expected, captured_error(Examples::APPLICATION_ERROR)
  end

  def test_non_retryable_fails_workflow_after_one_attempt
    with_worker([SagaWorkflow], Examples::NON_RETRYABLE) do |start|
      # The default Retry Policy would retry, but the error is non-retryable.
      handle = start.call(SagaWorkflow)
      err = assert_raises(Temporalio::Error::WorkflowFailedError) { handle.result }
      assert_equal 'Fail the Workflow', err.cause.message
      assert_equal 'Simulated failure', err.cause.cause.cause.message
      assert err.cause.cause.cause.non_retryable
      started = handle.fetch_history_events.filter_map(&:activity_task_started_event_attributes)
      assert_equal [1], started.map(&:attempt)
    end
  end
end
