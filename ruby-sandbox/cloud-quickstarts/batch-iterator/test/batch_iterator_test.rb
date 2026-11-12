# frozen_string_literal: true

require 'minitest/autorun'
require 'securerandom'
require 'temporalio/testing'
require 'temporalio/worker'
require_relative '../iterator_batch_workflow'
require_relative '../shared'

class BatchIteratorTest < Minitest::Test
  def test_batch_processes_every_record_across_continue_as_new
    total = BatchIterator::PAGE_SIZE * BatchIterator::GetRecordsActivity::PAGE_COUNT
    workflow_id = "batch-iterator-#{SecureRandom.uuid}"
    task_queue = "batch-iterator-#{SecureRandom.uuid}"

    # Time skipping makes the children's 1-3 second sleeps instant.
    Temporalio::Testing::WorkflowEnvironment.start_time_skipping do |env|
      worker = Temporalio::Worker.new(
        client: env.client,
        task_queue:,
        workflows: [BatchIterator::IteratorBatchWorkflow, BatchIterator::RecordProcessorWorkflow],
        activities: [BatchIterator::GetRecordsActivity]
      )
      worker.run do
        handle = env.client.start_workflow(
          BatchIterator::IteratorBatchWorkflow, BatchIterator::PAGE_SIZE, 0,
          id: workflow_id, task_queue:
        )
        assert_equal total, handle.result

        # The batch continued as new, so the latest run differs from the first.
        latest = env.client.workflow_handle(workflow_id).describe
        refute_equal handle.result_run_id, latest.run_id

        # Every record got its own completed child Workflow.
        total.times do |id|
          child = env.client.workflow_handle("#{workflow_id}/#{id}").describe
          assert_equal Temporalio::Client::WorkflowExecutionStatus::COMPLETED, child.status
        end
      end
    end
  end
end
