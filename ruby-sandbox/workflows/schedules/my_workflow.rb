# frozen_string_literal: true

require 'temporalio/workflow'

class MyWorkflow < Temporalio::Workflow::Definition
  def execute(input)
    "Received #{input}"
  end
end

class MyNewWorkflow < Temporalio::Workflow::Definition
  def execute(input)
    "Received #{input} in the new Workflow"
  end
end
