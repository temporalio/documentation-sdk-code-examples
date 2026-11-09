# frozen_string_literal: true

require 'temporalio/workflow'

# Uses the SDK's replay-safe alternatives for random values, time, and side effects.
class DeterminismWorkflow < Temporalio::Workflow::Definition
  def execute
    # @@@SNIPSTART ruby-workflow-basics-random
    value = Temporalio::Workflow.random.rand(1..100)
    unique_id = Temporalio::Workflow.random.uuid
    # @@@SNIPEND

    # @@@SNIPSTART ruby-workflow-basics-now
    current_time = Temporalio::Workflow.now
    # @@@SNIPEND

    # @@@SNIPSTART ruby-workflow-basics-replaying
    unless Temporalio::Workflow::Unsafe.replaying?
      emit_metric('workflow_started', 1)
    end
    # @@@SNIPEND

    { 'value' => value, 'unique_id' => unique_id, 'current_time' => current_time.iso8601 }
  end

  private

  def emit_metric(name, value)
    Temporalio::Workflow.metric_meter.create_metric(:counter, name).record(value)
  end
end
