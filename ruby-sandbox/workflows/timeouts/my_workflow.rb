# frozen_string_literal: true

require 'temporalio/workflow'

class MyWorkflow < Temporalio::Workflow::Definition
  def execute(input)
    "Received #{input}"
  end
end
