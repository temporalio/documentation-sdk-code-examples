# frozen_string_literal: true

require_relative 'test_helper'
require_relative '../greeting_client'

class GreetingTest < MessagePassingTest
  def test_greeting_workflow_messages
    with_worker(task_queue: 'message-passing-simple-sample', workflows: [GreetingWorkflow],
                activities: [CallGreetingService]) do
      handle = with_stubbed_connect { start_greeting_workflow }

      assert_equal %w[chinese english], query_languages(handle)
      assert_equal 'english', handle.query(GreetingWorkflow.language)
      assert_equal CallGreetingService.greetings.keys.sort.map(&:to_s),
                   handle.query(GreetingWorkflow.languages, { include_unsupported: true })

      assert_equal 'english', execute_set_language_update(handle)
      assert_equal 'chinese', start_apply_language_update(handle)

      # The validator rejects the Update, so it's never written to the Event History.
      assert_raises(Temporalio::Error::WorkflowUpdateFailedError) do
        handle.execute_update(GreetingWorkflow.set_language, :klingon)
      end
      # The Activity doesn't know the language, so the accepted Update fails.
      assert_raises(Temporalio::Error::WorkflowUpdateFailedError) do
        handle.execute_update(GreetingWorkflow.apply_language_with_lookup, :klingon)
      end

      send_approve_signal(handle)
      assert_equal CallGreetingService.greetings[:arabic], handle.result

      events = handle.fetch_history_events
      accepted = events.filter_map(&:workflow_execution_update_accepted_event_attributes)
      assert_equal %w[set_language apply_language_with_lookup apply_language_with_lookup],
                   accepted.map { |a| a.accepted_request.input.name }
      assert(events.any?(&:workflow_execution_signaled_event_attributes))
    end
  end
end
