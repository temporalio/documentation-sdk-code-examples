# frozen_string_literal: true

require 'temporalio/workflow'

# Defines its own MyWorkflow, so don't load it in the same process as the *_workflow.rb revisions in this folder.
# @@@SNIPSTART ruby-versioning-cutover-workflows
class MyWorkflow < Temporalio::Workflow::Definition
  def execute
    # ...
  end
end

class MyWorkflowV2 < Temporalio::Workflow::Definition
  def execute
    # ...
  end
end
# @@@SNIPEND
