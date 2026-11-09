# frozen_string_literal: true

require 'temporalio/workflow'

# Defines its own MyWorkflow, so don't load it in the same process as the other MyWorkflow files in this folder.
class MyWorkflow < Temporalio::Workflow::Definition
  def execute
    # Return as soon as an Update arrives, without waiting for it to finish
    Temporalio::Workflow.wait_condition { @update_started }
    'workflow-result'
  end

  # @@@SNIPSTART ruby-message-passing-unfinished-policy
  workflow_update unfinished_policy: Temporalio::Workflow::HandlerUnfinishedPolicy::ABANDON
  def my_update
    @update_started = true
    Temporalio::Workflow.wait_condition { false }
  end
  # @@@SNIPEND
end
