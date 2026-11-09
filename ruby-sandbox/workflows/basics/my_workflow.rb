# frozen_string_literal: true

require 'temporalio/workflow'
require_relative 'my_activity'

module Define
  # @@@SNIPSTART ruby-workflow-basics-define
  class MyWorkflow < Temporalio::Workflow::Definition
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
