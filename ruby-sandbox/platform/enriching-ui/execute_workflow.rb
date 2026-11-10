# frozen_string_literal: true

require 'temporalio/client'

def execute_your_workflow(client)
  # @@@SNIPSTART ruby-enriching-ui-execute-workflow
  # Execute workflow synchronously
  result = client.execute_workflow(
    'YourWorkflow',
    'workflow input',
    id: 'your-workflow-id',
    task_queue: 'your-task-queue',
    static_summary: 'Order processing for customer #12345',
    static_details: 'Processing premium order with expedited shipping'
  )
  # @@@SNIPEND
  result
end
