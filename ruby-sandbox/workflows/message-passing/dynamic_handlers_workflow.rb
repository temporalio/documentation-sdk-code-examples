# frozen_string_literal: true

require 'temporalio/workflow'

class DynamicHandlersWorkflow < Temporalio::Workflow::Definition
  workflow_query_attr_reader :pending_things

  def initialize
    @pending_things = []
  end

  def execute
    Temporalio::Workflow.wait_condition { @pending_things.size >= 2 }
    @pending_things
  end

  # @@@SNIPSTART ruby-message-passing-dynamic-query
  workflow_query dynamic: true, raw_args: true
  def dynamic_query(query_name, *args)
    first_param = Temporalio::Workflow.payload_converter.from_payload(
      (args.first || raise('Missing first parameter')).payload
    )
    "Got parameter #{first_param} for query #{query_name}"
  end
  # @@@SNIPEND

  # @@@SNIPSTART ruby-message-passing-dynamic-signal
  workflow_signal dynamic: true, raw_args: true
  def dynamic_signal(signal_name, *args)
    first_param = Temporalio::Workflow.payload_converter.from_payload(
      (args.first || raise('Missing first parameter')).payload
    )
    @pending_things << "Got parameter #{first_param} for signal #{signal_name}"
  end
  # @@@SNIPEND

  # @@@SNIPSTART ruby-message-passing-dynamic-update
  workflow_update dynamic: true, raw_args: true
  def dynamic_update(update_name, *args)
    first_param = Temporalio::Workflow.payload_converter.from_payload(
      (args.first || raise('Missing first parameter')).payload
    )
    @pending_things << "Got parameter #{first_param} for update #{update_name}"
  end
  # @@@SNIPEND
end
