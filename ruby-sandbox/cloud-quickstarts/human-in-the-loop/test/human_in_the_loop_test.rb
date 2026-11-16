# frozen_string_literal: true

require 'minitest/autorun'
require 'securerandom'
require 'temporalio/testing'
require 'temporalio/worker'
require_relative '../human_in_the_loop_workflow'

class HumanInTheLoopTest < Minitest::Test
  SAFE = {
    'action_type' => 'list_files',
    'description' => 'List directory contents',
    'reasoning' => 'Read-only operation',
    'risky_action' => false
  }.freeze
  RISKY = {
    'action_type' => 'delete_data',
    'description' => 'Delete production data',
    'reasoning' => 'User asked for deletion',
    'risky_action' => true
  }.freeze

  # Stand-ins for the real Activities, registered under the same names, so the
  # tests need no API key.
  class MockProposeAction < Temporalio::Activity::Definition
    activity_name 'ProposeActionActivity'

    def initialize(action)
      @action = action
    end

    def execute(_request)
      @action
    end
  end

  class MockNotify < Temporalio::Activity::Definition
    activity_name 'NotifyApprovalNeededActivity'

    def initialize(requests)
      @requests = requests
    end

    def execute(request)
      @requests << request
      nil
    end
  end

  class MockExecute < Temporalio::Activity::Definition
    activity_name 'ExecuteActionActivity'

    def execute(action)
      "Executed: #{action['action_type']}"
    end
  end

  # Runs the Workflow with mocked Activities. If decide is set, sends it as the
  # reviewer's [approved, notes] once approval is requested.
  def run_workflow(action, timeout_seconds, decide: nil, with_request_id: true)
    requests = Queue.new
    task_queue = "human-in-the-loop-#{SecureRandom.uuid}"

    # Time skipping makes the approval timeout fire instantly.
    Temporalio::Testing::WorkflowEnvironment.start_time_skipping do |env|
      worker = Temporalio::Worker.new(
        client: env.client,
        task_queue:,
        workflows: [HumanInTheLoop::HumanInTheLoopWorkflow],
        activities: [MockProposeAction.new(action), MockNotify.new(requests), MockExecute]
      )
      worker.run do
        handle = env.client.start_workflow(
          HumanInTheLoop::HumanInTheLoopWorkflow,
          { 'user_request' => 'test', 'approval_timeout_seconds' => timeout_seconds },
          id: "human-in-the-loop-#{SecureRandom.uuid}",
          task_queue:,
          run_timeout: 30
        )
        if decide
          request = requests.pop(timeout: 10) or flunk('approval was never requested')
          approved, notes = decide
          handle.signal(
            HumanInTheLoop::HumanInTheLoopWorkflow.approval_decision,
            {
              'approved' => approved,
              'request_id' => with_request_id ? request['request_id'] : nil,
              'reviewer_notes' => notes,
              'decided_at' => '2026-01-01T00:00:00Z'
            }
          )
        end
        handle.result
      end
    end
  end

  def test_safe_action_is_auto_approved
    assert_equal 'Action completed successfully (auto-approved): Executed: list_files',
                 run_workflow(SAFE, 30)
  end

  def test_risky_action_runs_when_approved
    assert_equal 'Action completed successfully: Executed: delete_data',
                 run_workflow(RISKY, 300, decide: [true, 'Looks good'])
  end

  def test_decision_without_request_id_applies_to_pending_request
    assert_equal 'Action completed successfully: Executed: delete_data',
                 run_workflow(RISKY, 300, decide: [true, 'Looks good'], with_request_id: false)
  end

  def test_risky_action_is_rejected
    assert_equal 'Action rejected. Reviewer notes: Too risky',
                 run_workflow(RISKY, 300, decide: [false, 'Too risky'])
  end

  def test_risky_action_times_out_without_a_decision
    assert_includes run_workflow(RISKY, 1), 'timed out after 1 seconds'
  end

  def test_propose_action_returns_risky_mock_without_api_key
    saved = ENV.delete('OPENAI_API_KEY')
    action = Temporalio::Testing::ActivityEnvironment.new.run(
      HumanInTheLoop::ProposeActionActivity,
      { 'model' => 'gpt-4o-mini', 'instructions' => '', 'input' => 'test' }
    )
    assert_equal true, action['risky_action']
    assert_equal 'delete_test_data', action['action_type']
  ensure
    ENV['OPENAI_API_KEY'] = saved if saved
  end
end
