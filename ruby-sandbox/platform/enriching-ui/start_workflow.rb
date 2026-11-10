# frozen_string_literal: true

# Starts YourWorkflow (any of the Workflow files in this folder) on a Worker
# polling your-task-queue.

# @@@SNIPSTART ruby-enriching-ui-start-workflow
require 'temporalio/client'

# Create client
client = Temporalio::Client.connect('localhost:7233', 'default')

# Start a workflow with static summary and details
handle = client.start_workflow(
  'YourWorkflow',
  'workflow input',
  id: 'your-workflow-id',
  task_queue: 'your-task-queue',
  static_summary: 'Order processing for customer #12345',
  static_details: 'Processing premium order with expedited shipping'
)
# @@@SNIPEND

puts "Started #{handle.id}"
