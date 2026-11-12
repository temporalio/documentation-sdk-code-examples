# frozen_string_literal: true

require 'temporalio/client'
require_relative 'connection'
require_relative 'iterator_batch_workflow'
require_relative 'shared'

args, kwargs = BatchIterator.connect_options
client = Temporalio::Client.connect(*args, **kwargs)

handle = client.start_workflow(
  BatchIterator::IteratorBatchWorkflow, BatchIterator::PAGE_SIZE, 0,
  id: "batch-iterator-#{(Time.now.to_f * 1000).to_i}",
  task_queue: BatchIterator::TASK_QUEUE
)
puts "Started batch Workflow #{handle.id} against #{args[0]} (namespace: #{args[1]})..."

# result follows the continue-as-new chain and returns the final run's result.
processed = handle.result
puts "Batch complete: processed #{processed} records"
