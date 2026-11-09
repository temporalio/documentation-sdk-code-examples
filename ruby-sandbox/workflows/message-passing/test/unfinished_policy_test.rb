# frozen_string_literal: true

require_relative 'test_helper'
require_relative '../unfinished_policy_workflow'

class UnfinishedPolicyTest < MessagePassingTest
  # The SDK writes the unfinished-handler warning with Kernel#warn, so capture $stderr.
  def capture_stderr
    original = $stderr
    $stderr = StringIO.new
    yield
    $stderr.string
  ensure
    $stderr = original
  end

  def test_abandon_policy_silences_warning
    output = capture_stderr do
      with_worker(workflows: [MyWorkflow]) do
        handle = start(MyWorkflow)
        handle.start_update(MyWorkflow.my_update,
                            wait_for_stage: Temporalio::Client::WorkflowUpdateWaitStage::ACCEPTED)
        assert_equal 'workflow-result', handle.result
      end
    end
    refute_match(/unfinished/, output)
  end
end
