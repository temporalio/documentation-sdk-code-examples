# frozen_string_literal: true

require_relative 'test_helper'
require_relative '../handler_concurrency_workflow'

class HandlerConcurrencyTest < MessagePassingTest
  def wait_for(handle)
    deadline = Time.now + 15
    sleep 0.2 until yield(handle) || Time.now > deadline
  end

  def test_safe_handler_keeps_x_and_y_together
    with_worker(workflows: [MyWorkflow], activities: [FetchDataActivity.new]) do
      handle = start(MyWorkflow)
      2.times { handle.signal(MyWorkflow.safe_handler) }
      wait_for(handle) { |h| h.query(MyWorkflow.y) == 2 }
      assert_equal [2, 2], [handle.query(MyWorkflow.x), handle.query(MyWorkflow.y)]
      handle.terminate
    end
  end

  def test_bad_handler_runs
    with_worker(workflows: [MyWorkflow], activities: [FetchDataActivity.new]) do
      handle = start(MyWorkflow)
      handle.signal(MyWorkflow.bad_handler)
      wait_for(handle) { |h| h.query(MyWorkflow.y) == 1 }
      assert_equal 1, handle.query(MyWorkflow.x)
      handle.terminate
    end
  end
end
