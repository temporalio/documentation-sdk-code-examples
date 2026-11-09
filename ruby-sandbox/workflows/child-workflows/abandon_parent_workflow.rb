# frozen_string_literal: true

require 'temporalio/workflow'
require_relative 'my_child_workflow'

# Starts a Child Workflow that keeps running if this Workflow closes first.
class AbandonParentWorkflow < Temporalio::Workflow::Definition
  def execute
    # @@@SNIPSTART ruby-child-workflows-parent-close-policy
    Temporalio::Workflow.execute_child_workflow(
      MyChildWorkflow,
      'my-workflow-arg',
      parent_close_policy: Temporalio::Workflow::ParentClosePolicy::ABANDON
    )
    # @@@SNIPEND
  end
end
