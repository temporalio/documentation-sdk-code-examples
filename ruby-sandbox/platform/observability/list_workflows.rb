# frozen_string_literal: true

require 'temporalio/client'

def list_greeting_workflows(my_client)
  # @@@SNIPSTART ruby-observability-list-workflows
  my_client.list_workflows("WorkflowType='GreetingWorkflow'").each do |wf|
    puts "Workflow: #{wf.id}"
  end
  # @@@SNIPEND
end
