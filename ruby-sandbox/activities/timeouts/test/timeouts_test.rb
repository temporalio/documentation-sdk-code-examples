# frozen_string_literal: true

require 'minitest/autorun'
require 'securerandom'
require 'temporalio/testing'
require 'temporalio/worker'
require_relative '../workflows'
require_relative '../next_retry_delay_activity'
require_relative '../heartbeat_activity'

# Retries slowly by policy, so a fast retry shows next_retry_delay overrode it.
class NextRetryDelayWorkflow < Temporalio::Workflow::Definition
  def execute
    Temporalio::Workflow.execute_activity(
      NextRetryDelayActivity,
      start_to_close_timeout: 10,
      retry_policy: Temporalio::RetryPolicy.new(initial_interval: 60)
    )
  end
end

# Runs the heartbeating Activity past its Heartbeat Timeout. It fails only when
# the Start-To-Close Timeout is reached.
class HeartbeatWorkflow < Temporalio::Workflow::Definition
  def execute
    Temporalio::Workflow.execute_activity(
      Heartbeat::MyActivity,
      start_to_close_timeout: 8,
      heartbeat_timeout: 5,
      retry_policy: Temporalio::RetryPolicy.new(max_attempts: 1)
    )
  end
end

class ActivityTimeoutsTest < Minitest::Test
  def with_worker(workflows, activities)
    Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ) do |env|
      task_queue = "activity-timeouts-#{SecureRandom.uuid}"
      Temporalio::Worker.new(client: env.client, task_queue:, workflows:, activities:).run do
        yield ->(wf, *args) { env.client.start_workflow(wf, *args, id: "wf-#{SecureRandom.uuid}", task_queue:) }
      end
    end
  end

  def scheduled(handle)
    handle.fetch_history_events.find(&:activity_task_scheduled_event_attributes)
          .activity_task_scheduled_event_attributes
  end

  def test_timeouts_and_retry_policy
    with_worker([StartToCloseWorkflow, RetryPolicyWorkflow, HeartbeatTimeoutWorkflow], [MyActivity]) do |start|
      h = start.call(StartToCloseWorkflow, 'Temporal')
      assert_equal 'Hello, Temporal!', h.result
      assert_equal 300, scheduled(h).start_to_close_timeout.seconds

      h = start.call(RetryPolicyWorkflow, 'Temporal')
      assert_equal 'Hello, Temporal!', h.result
      assert_equal 10, scheduled(h).retry_policy.maximum_interval.seconds

      h = start.call(HeartbeatTimeoutWorkflow, 'Temporal')
      assert_equal 'Hello, Temporal!', h.result
      assert_equal 5, scheduled(h).heartbeat_timeout.seconds
    end
  end

  def test_next_retry_delay
    with_worker([NextRetryDelayWorkflow], [NextRetryDelayActivity]) do |start|
      started = Time.now
      assert_equal 'done', start.call(NextRetryDelayWorkflow).result
      # Attempt 1 sets a 3-second delay instead of the policy's 60 seconds.
      elapsed = Time.now - started
      assert_operator elapsed, :>=, 3
      assert_operator elapsed, :<, 20
    end
  end

  def test_heartbeats_keep_activity_alive
    with_worker([HeartbeatWorkflow], [Heartbeat::MyActivity]) do |start|
      err = assert_raises(Temporalio::Error::WorkflowFailedError) { start.call(HeartbeatWorkflow).result }
      timeout = err.cause.cause
      assert_instance_of Temporalio::Error::TimeoutError, timeout
      assert_equal Temporalio::Error::TimeoutError::TimeoutType::START_TO_CLOSE, timeout.type
    end
  end
end
