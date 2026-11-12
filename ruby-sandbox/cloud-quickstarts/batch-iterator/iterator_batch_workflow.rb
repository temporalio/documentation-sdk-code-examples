# frozen_string_literal: true

require 'temporalio/workflow'
require_relative 'get_records_activity'
require_relative 'record_processor_workflow'

module BatchIterator
  # Implements the iterator pattern.
  #
  # A single Workflow run processes one page of records in parallel, starting one
  # RecordProcessorWorkflow child per record. After every child completes, the
  # Workflow continues as new with the next offset, so each run's Event History
  # stays small no matter how large the dataset is.
  class IteratorBatchWorkflow < Temporalio::Workflow::Definition
    # page_size is the number of records to process in a single Workflow run.
    # offset is the offset of the first record to process; 0 starts the batch.
    # Returns the total number of processed records.
    def execute(page_size, offset)
      records = Temporalio::Workflow.execute_activity(
        GetRecordsActivity, page_size, offset,
        start_to_close_timeout: 5
      )

      # No more records in the dataset: the batch is done.
      return offset if records.empty?

      # Start one child per record with a human-friendly ID, e.g.
      # "batch-iterator-123/7", then wait for every child in this page. Error
      # handling is skipped for brevity.
      children = records.map do |record|
        Temporalio::Workflow::Future.new do
          Temporalio::Workflow.execute_child_workflow(
            RecordProcessorWorkflow, record,
            id: "#{Temporalio::Workflow.info.workflow_id}/#{record['id']}"
          )
        end
      end
      Temporalio::Workflow::Future.all_of(*children).wait

      raise Temporalio::Workflow::ContinueAsNewError.new(page_size, offset + records.size)
    end
  end
end
