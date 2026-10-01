require 'temporalio/client'
require_relative 'say_hello_workflow'

# Create a client
client = Temporalio::Client.connect('localhost:7233', 'default')

# Run workflow
result = client.execute_workflow(
  SayHelloWorkflow,
  'Temporal', # This is the input to the workflow
  id: 'my-workflow-id',
  task_queue: 'my-task-queue'
)
puts "Result: #{result}"
