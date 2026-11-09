# frozen_string_literal: true

require 'temporalio/workflow'

class MyChildWorkflow < Temporalio::Workflow::Definition
  def execute(arg)
    "Child received #{arg}"
  end
end
