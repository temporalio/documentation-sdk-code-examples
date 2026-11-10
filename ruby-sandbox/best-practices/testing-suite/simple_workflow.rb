# frozen_string_literal: true

require 'temporalio/workflow'

# @@@SNIPSTART ruby-testing-simple-workflow
class SimpleWorkflow < Temporalio::Workflow::Definition
  def execute(name)
    "Hello, #{name}!"
  end
end
# @@@SNIPEND
