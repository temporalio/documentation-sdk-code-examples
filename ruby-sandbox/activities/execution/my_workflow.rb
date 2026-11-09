# frozen_string_literal: true

require 'temporalio/workflow'
require_relative '../basics/my_activity'

# @@@SNIPSTART ruby-activity-execution-execute
class MyWorkflow < Temporalio::Workflow::Definition
  # Customize the name
  workflow_name :MyDifferentWorkflowName

  def execute(name)
    Temporalio::Workflow.execute_activity(
      MyActivity,
      { greeting: 'Hello', name: },
      start_to_close_timeout: 100
    )
  end
end
# @@@SNIPEND
