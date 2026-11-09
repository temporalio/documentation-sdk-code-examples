# frozen_string_literal: true

require 'minitest/autorun'
require 'securerandom'
require 'temporalio/testing'
require 'temporalio/worker'
require_relative '../timer_workflow'

class TimersTest < Minitest::Test
  def test_timer_duration_and_summary
    Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ) do |env|
      task_queue = "timers-#{SecureRandom.uuid}"
      Temporalio::Worker.new(client: env.client, task_queue:, workflows: [TimerWorkflow]).run do
        handle = env.client.start_workflow(TimerWorkflow, id: "wf-#{SecureRandom.uuid}", task_queue:)
        event = nil
        deadline = Time.now + 10
        until (event = handle.fetch_history_events.find(&:timer_started_event_attributes))
          raise 'Timer did not start' if Time.now > deadline

          sleep 0.1
        end

        assert_equal 72 * 60 * 60, event.timer_started_event_attributes.start_to_fire_timeout.seconds
        summary = env.client.data_converter.payload_converter.from_payload(event.user_metadata.summary)
        assert_equal 'my timer', summary
        handle.terminate
      end
    end
  end

  # The time-skipping test server fires the 72-hour Timer at once.
  def test_timer_fires
    Temporalio::Testing::WorkflowEnvironment.start_time_skipping do |env|
      task_queue = "timers-#{SecureRandom.uuid}"
      Temporalio::Worker.new(client: env.client, task_queue:, workflows: [TimerWorkflow]).run do
        assert_equal 'Timer fired',
                     env.client.execute_workflow(TimerWorkflow, id: "wf-#{SecureRandom.uuid}", task_queue:)
      end
    end
  end
end
