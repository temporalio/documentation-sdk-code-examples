# frozen_string_literal: true

require 'logger'
require 'temporalio/client'
require 'temporalio/worker'
require_relative 'activities'
require_relative 'connection'
require_relative 'human_in_the_loop_workflow'
require_relative 'shared'

args, kwargs = HumanInTheLoop.connect_options
# Show the Workflow and Activity log lines in this terminal.
client = Temporalio::Client.connect(*args, **kwargs, logger: Logger.new($stdout, level: Logger::INFO))

worker = Temporalio::Worker.new(
  client:,
  task_queue: HumanInTheLoop::TASK_QUEUE,
  workflows: [HumanInTheLoop::HumanInTheLoopWorkflow],
  activities: [
    HumanInTheLoop::ProposeActionActivity,
    HumanInTheLoop::NotifyApprovalNeededActivity,
    HumanInTheLoop::ExecuteActionActivity
  ]
)

puts "Worker connected to #{args[0]} (namespace: #{args[1]}), " \
     "polling task queue '#{HumanInTheLoop::TASK_QUEUE}' (ctrl+c to exit)"
worker.run(shutdown_signals: ['SIGINT'])
