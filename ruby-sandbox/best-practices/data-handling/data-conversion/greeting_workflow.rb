# frozen_string_literal: true

require 'temporalio/activity'
require 'temporalio/workflow'
require_relative 'greeting_input'

class ComposeGreeting < Temporalio::Activity::Definition
  def execute(input)
    GreetingInput.new(name: "Hello, #{input.name}!", count: input.count + 1)
  end
end

class GreetingWorkflow < Temporalio::Workflow::Definition
  def execute(input)
    Temporalio::Workflow.execute_activity(ComposeGreeting, input, start_to_close_timeout: 10)
  end
end
