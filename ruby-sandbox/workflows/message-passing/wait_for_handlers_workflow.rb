# frozen_string_literal: true

require 'temporalio/workflow'

# Defines its own MyWorkflow, so don't load it in the same process as the other MyWorkflow files in this folder.
# The page shows parts of this class with selectedLines, so execute must be the last method.
# @@@SNIPSTART ruby-message-passing-wait-for-handlers
class MyWorkflow < Temporalio::Workflow::Definition
  workflow_update
  def finish
    # An async handler that is still running when the main Workflow method is ready to return
    @finish_requested = true
    Temporalio::Workflow.sleep(1)
    'finished'
  end

  def execute
    Temporalio::Workflow.wait_condition { @finish_requested }

    Temporalio::Workflow.wait_condition { Temporalio::Workflow.all_handlers_finished? }
    'workflow-result'
  end
end
# @@@SNIPEND
