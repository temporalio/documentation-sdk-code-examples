# frozen_string_literal: true

require 'temporalio/workflow'

# The Workflow whose histories the replay examples check.
class MyWorkflow < Temporalio::Workflow::Definition
  def execute(name)
    Temporalio::Workflow.sleep(1)
    "Hello, #{name}!"
  end
end
