# frozen_string_literal: true

require 'temporalio/client'
require_relative 'my_workflow'

def execute_my_workflow(my_client)
  # @@@SNIPSTART ruby-client-execute-workflow
  result = my_client.execute_workflow(
    MyWorkflow, 'some-input',
    id: 'my-workflow-id', task_queue: 'my-task-queue'
  )
  puts "Result: #{result}"
  # @@@SNIPEND
end

def execute_with_options(my_client)
  # @@@SNIPSTART ruby-client-workflow-options
  result = my_client.execute_workflow(
    MyWorkflow, 'some-input',
    id: 'my-workflow-id', task_queue: 'my-task-queue'
  )
  # @@@SNIPEND
  result
end

def get_workflow_result(my_client)
  # @@@SNIPSTART ruby-client-get-result
  handle = my_client.workflow_handle('my-workflow-id')
  result = handle.result
  puts "Result: #{result}"
  # @@@SNIPEND
end
