# frozen_string_literal: true

require_relative 'test_helper'
require_relative '../wait_for_handlers_workflow'

class WaitForHandlersTest < MessagePassingTest
  def test_workflow_waits_for_handler
    with_worker(workflows: [MyWorkflow]) do
      handle = start(MyWorkflow)
      # Without the all_handlers_finished? wait, the Workflow would complete during the handler's sleep and this
      # Update would fail.
      assert_equal 'finished', handle.execute_update(MyWorkflow.finish)
      assert_equal 'workflow-result', handle.result
    end
  end
end
