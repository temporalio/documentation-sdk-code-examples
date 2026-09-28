require 'temporalio/client'
require 'temporalio/worker'
require_relative 'say_hello_activity'
require_relative 'say_hello_workflow'

# Create a client
client = Temporalio::Client.connect('localhost:7233', 'default')

# Create a worker with the client, activities, and workflows
worker = Temporalio::Worker.new(
  client:,
  task_queue: 'my-task-queue',
  workflows: [SayHelloWorkflow],
  # There are various forms an activity can take, see "Activities" section for details
  activities: [SayHelloActivity]
)

# Run the worker until SIGINT. This can be done in many ways, see "Workers" section for details.
worker.run(shutdown_signals: ['SIGINT'])