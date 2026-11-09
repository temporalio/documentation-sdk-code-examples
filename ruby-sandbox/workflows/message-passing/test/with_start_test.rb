# frozen_string_literal: true

require_relative 'test_helper'
require_relative '../with_start'

class WithStartTest < MessagePassingTest
  def test_signal_with_start_then_update_with_start
    with_worker(task_queue: 'my-workflow-task-queue', workflows: [MyWorkflow]) do
      with_stubbed_connect do
        handle = signal_with_start
        assert_equal 'my-workflow-input, signal: signal-input, update: ', handle.result

        update_result, workflow_result = update_with_start
        assert_equal 'Received update-input', update_result
        assert_equal 'my-workflow-input, signal: , update: update-input', workflow_result
      end
    end
  end
end
