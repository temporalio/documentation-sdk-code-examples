# frozen_string_literal: true

require 'temporalio/workflow'
require_relative 'my_child_workflow'

# Starts a Child Workflow with the default Parent Close Policy (TERMINATE) and waits for its result.
class ParentWorkflow < Temporalio::Workflow::Definition
  def execute
    # @@@SNIPSTART ruby-child-workflows-execute
    Temporalio::Workflow.execute_child_workflow(MyChildWorkflow, 'my-workflow-arg')
    # @@@SNIPEND
  end
end
