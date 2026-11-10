# frozen_string_literal: true

require 'temporalio/workflow'

# The module keeps this GreetingWorkflow apart from the one the Worker runs.
module PerWorkflowBehavior
  # @@@SNIPSTART ruby-cloud-run-workflow-versioning-behavior
  class GreetingWorkflow < Temporalio::Workflow::Definition
    workflow_versioning_behavior Temporalio::VersioningBehavior::PINNED

    def execute(name)
      # ...
    end
  end
  # @@@SNIPEND
end
