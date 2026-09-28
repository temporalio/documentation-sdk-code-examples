require 'temporalio/workflow'
require_relative 'say_hello_activity'

class SayHelloWorkflow < Temporalio::Workflow::Definition
  def execute(name)
    Temporalio::Workflow.execute_activity(
      SayHelloActivity,
      name,
      schedule_to_close_timeout: 300
    )
  end
end