# frozen_string_literal: true

require 'temporalio/workflow'

# Waits for some_signal and returns its argument.
class WorkflowA < Temporalio::Workflow::Definition
  def execute
    Temporalio::Workflow.wait_condition { @signal_arg }
  end

  workflow_signal
  def some_signal(arg)
    @signal_arg = arg
  end
end

# @@@SNIPSTART ruby-message-passing-signal-from-workflow
class WorkflowB < Temporalio::Workflow::Definition
  def execute
    handle = Temporalio::Workflow.external_workflow_handle('workflow-a-id')
    handle.signal(WorkflowA.some_signal, 'some signal arg')
  end
end
# @@@SNIPEND
