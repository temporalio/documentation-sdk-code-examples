# frozen_string_literal: true

require 'temporalio/workflow'

# Defines its own MyWorkflow, so don't load it in the same process as the other MyWorkflow files in this folder.
class MyWorkflow < Temporalio::Workflow::Definition
  def execute
    Temporalio::Workflow.wait_condition { @processed&.size == 2 }
    @processed
  end

  # @@@SNIPSTART ruby-message-passing-wait-in-handler
  workflow_update
  def my_update(my_update_input)
    Temporalio::Workflow.wait_condition { ready_for_update_to_execute(my_update_input) }
    (@processed ||= []) << my_update_input
  end
  # @@@SNIPEND

  workflow_signal
  def open_gate
    @gate_open = true
  end

  private

  # Updates wait until the open_gate Signal arrives.
  def ready_for_update_to_execute(_my_update_input)
    @gate_open
  end
end
