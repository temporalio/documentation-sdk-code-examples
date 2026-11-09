# frozen_string_literal: true

require 'temporalio/client'
require 'temporalio/retry_policy'
require_relative 'my_workflow'

def execute_with_timeout(my_client)
  # @@@SNIPSTART ruby-workflow-timeouts-execution-timeout
  result = my_client.execute_workflow(
    MyWorkflow, 'some-input',
    id: 'my-workflow-id', task_queue: 'my-task-queue',
    execution_timeout: 5 * 60
  )
  # @@@SNIPEND
  result
end

def execute_with_retry_policy(my_client)
  # @@@SNIPSTART ruby-workflow-timeouts-retry-policy
  result = my_client.execute_workflow(
    MyWorkflow, 'some-input',
    id: 'my-workflow-id', task_queue: 'my-task-queue',
    retry_policy: Temporalio::RetryPolicy.new(max_interval: 10)
  )
  # @@@SNIPEND
  result
end
