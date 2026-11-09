# frozen_string_literal: true

require_relative 'test_helper'
require_relative '../external_signal_workflows'

class ExternalSignalTest < MessagePassingTest
  def test_signal_from_workflow
    with_worker(workflows: [WorkflowA, WorkflowB]) do
      workflow_a = client.start_workflow(WorkflowA, id: 'workflow-a-id', task_queue: @task_queue)
      workflow_b = start(WorkflowB)
      workflow_b.result

      assert_equal 'some signal arg', workflow_a.result
      assert(workflow_b.fetch_history_events.any?(&:signal_external_workflow_execution_initiated_event_attributes))
      assert(workflow_a.fetch_history_events.any?(&:workflow_execution_signaled_event_attributes))
    end
  end
end
