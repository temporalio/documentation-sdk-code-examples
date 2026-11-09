# frozen_string_literal: true

require 'temporalio/workflow'
require_relative 'my_activity'

module CustomName
  # @@@SNIPSTART ruby-workflow-basics-custom-name
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
end
