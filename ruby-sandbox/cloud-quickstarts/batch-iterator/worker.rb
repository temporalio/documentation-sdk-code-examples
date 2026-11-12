# frozen_string_literal: true

require 'logger'
require 'temporalio/client'
require 'temporalio/worker'
require_relative 'connection'
require_relative 'iterator_batch_workflow'
require_relative 'shared'

args, kwargs = BatchIterator.connect_options
# Show the "Processed record N" lines the child Workflows log.
client = Temporalio::Client.connect(*args, **kwargs, logger: Logger.new($stdout, level: Logger::INFO))

worker = Temporalio::Worker.new(
  client:,
  task_queue: BatchIterator::TASK_QUEUE,
  workflows: [BatchIterator::IteratorBatchWorkflow, BatchIterator::RecordProcessorWorkflow],
  activities: [BatchIterator::GetRecordsActivity]
)

puts "Worker connected to #{args[0]} (namespace: #{args[1]}), " \
     "polling task queue '#{BatchIterator::TASK_QUEUE}' (ctrl+c to exit)"
worker.run(shutdown_signals: ['SIGINT'])
