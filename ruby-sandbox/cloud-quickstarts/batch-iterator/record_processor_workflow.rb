# frozen_string_literal: true

require 'temporalio/workflow'

module BatchIterator
  # Child Workflow that processes a single record.
  class RecordProcessorWorkflow < Temporalio::Workflow::Definition
    def execute(record)
      # Simulate 1-3 seconds of work. Kept short so the whole batch finishes in
      # seconds. Workflow.random is deterministic on replay, unlike Kernel#rand.
      Temporalio::Workflow.sleep(Temporalio::Workflow.random.rand(1..3))
      Temporalio::Workflow.logger.info("Processed record #{record['id']}")
    end
  end
end
