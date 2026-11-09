# frozen_string_literal: true

require 'temporalio/workflow'
require_relative 'call_greeting_service'

# The page shows parts of this class with selectedLines, so the line order matters: language must follow the class
# line, and apply_language_with_lookup must be the last method.
# @@@SNIPSTART ruby-message-passing-greeting-workflow
class GreetingWorkflow < Temporalio::Workflow::Definition
  # This is the equivalent of:
  #    workflow_query
  #    def language
  #      @language
  #    end
  workflow_query_attr_reader :language

  def initialize
    @greetings = {
      chinese: '你好，世界',
      english: 'Hello, world'
    }
    @language = :english
  end

  def execute
    # Wait until approved and all handlers are finished
    Temporalio::Workflow.wait_condition { @approved_for_release && Temporalio::Workflow.all_handlers_finished? }
    @greetings[@language]
  end

  workflow_query
  def languages(input)
    # A query handler returns a value: it can inspect but must not mutate the Workflow state.
    if input['include_unsupported']
      CallGreetingService.greetings.keys.sort
    else
      @greetings.keys.sort
    end
  end

  workflow_signal
  def approve(input)
    # A signal handler mutates the workflow state but cannot return a value.
    @approved_for_release = true
    @approver_name = input['name']
  end

  workflow_update
  def set_language(new_language) # rubocop:disable Naming/AccessorMethodName
    # An update handler can mutate the workflow state and return a value.
    prev = @language.to_sym
    @language = new_language.to_sym
    prev
  end

  workflow_update_validator(:set_language)
  def validate_set_language(new_language)
    # In an update validator you raise any exception to reject the update.
    raise "#{new_language} is not supported" unless @greetings.include?(new_language.to_sym)
  end

  workflow_update
  def apply_language_with_lookup(new_language)
    # Call an activity if it's not there.
    unless @greetings.include?(new_language.to_sym)
      # We use a mutex so that, if this handler is executed multiple times, each execution
      # can schedule the activity only when the previously scheduled activity has
      # completed. This ensures that multiple calls to apply_language_with_lookup are
      # processed in order.
      @apply_language_mutex ||= Temporalio::Workflow::Mutex.new
      @apply_language_mutex.synchronize do
        greeting = Temporalio::Workflow.execute_activity(
          CallGreetingService, new_language, start_to_close_timeout: 10
        )
        # The requested language might not be supported by the remote service. If so, we
        # raise ApplicationError, which will fail the update. The
        # WorkflowExecutionUpdateAccepted event will still be added to history. (Update
        # validators can be used to reject updates before any event is written to history,
        # but they cannot be async, and so we cannot use an update validator for this
        # purpose.)
        raise Temporalio::Error::ApplicationError, "Greeting service does not support #{new_language}" unless greeting

        @greetings[new_language.to_sym] = greeting
      end
    end
    set_language(new_language)
  end
end
# @@@SNIPEND
