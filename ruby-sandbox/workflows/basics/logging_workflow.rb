# frozen_string_literal: true

require 'temporalio/workflow'

module Logging
  # @@@SNIPSTART ruby-workflow-basics-logging
  class MyWorkflow < Temporalio::Workflow::Definition
    def execute(name)
      Temporalio::Workflow.logger.info("Starting workflow for #{name}")
      # ...
    end
  end
  # @@@SNIPEND
end
