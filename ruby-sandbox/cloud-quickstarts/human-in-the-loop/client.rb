# frozen_string_literal: true

require 'temporalio/client'
require_relative 'connection'
require_relative 'human_in_the_loop_workflow'
require_relative 'shared'

DEFAULT_REQUEST = 'Delete all test data from the production database'

user_request = ARGV[0] || DEFAULT_REQUEST

args, kwargs = HumanInTheLoop.connect_options
client = Temporalio::Client.connect(*args, **kwargs)

begin
  handle = client.start_workflow(
    HumanInTheLoop::HumanInTheLoopWorkflow,
    { 'user_request' => user_request, 'approval_timeout_seconds' => 300 },
    id: HumanInTheLoop::WORKFLOW_ID,
    task_queue: HumanInTheLoop::TASK_QUEUE
  )
rescue Temporalio::Error::WorkflowAlreadyStartedError
  puts "Workflow #{HumanInTheLoop::WORKFLOW_ID} is already running. Approve or reject it, wait " \
       "for it to time out, or terminate it:\n" \
       "  temporal workflow terminate --workflow-id #{HumanInTheLoop::WORKFLOW_ID}"
  exit 1
end

puts "Started Workflow #{handle.id} against #{args[0]} (namespace: #{args[1]})..."
puts "Request: #{user_request}"
puts 'If the action is risky, the Worker prints how to approve or reject it.'

puts "Result: #{handle.result}"
