# frozen_string_literal: true

require 'temporalio/workflow'
require_relative 'say_hello'

# Sets no versioning behavior of its own. The Worker's
# default_versioning_behavior covers it.
class GreetingWorkflow < Temporalio::Workflow::Definition
  def execute(name)
    Temporalio::Workflow.execute_activity(SayHello, name, start_to_close_timeout: 10)
  end
end
