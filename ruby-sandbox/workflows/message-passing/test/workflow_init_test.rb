# frozen_string_literal: true

require_relative 'test_helper'
require_relative '../workflow_init_workflow'

class WorkflowInitTest < MessagePassingTest
  def test_handler_sees_input_from_constructor
    with_worker(workflows: [WorkflowInitWorkflow], activities: [CheckTitleValidityActivity]) do
      handle = start(WorkflowInitWorkflow, { 'name' => 'Galahad' })
      assert_equal true, handle.execute_update(WorkflowInitWorkflow.check_title_validity)
      assert_equal 'Hello, Sir Galahad', handle.result
    end
  end
end
