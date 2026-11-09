# frozen_string_literal: true

require_relative 'test_helper'
require_relative '../wait_in_handler_workflow'

class WaitInHandlerTest < MessagePassingTest
  def test_update_waits_for_condition
    with_worker(workflows: [MyWorkflow]) do
      handle = start(MyWorkflow)
      first = handle.start_update(MyWorkflow.my_update, 'a',
                                  wait_for_stage: Temporalio::Client::WorkflowUpdateWaitStage::ACCEPTED)
      second = handle.start_update(MyWorkflow.my_update, 'b',
                                   wait_for_stage: Temporalio::Client::WorkflowUpdateWaitStage::ACCEPTED)
      refute(first.result_obtained?)

      handle.signal(MyWorkflow.open_gate)
      assert_equal %w[a], first.result
      assert_equal %w[a b], second.result
      assert_equal %w[a b], handle.result
    end
  end
end
