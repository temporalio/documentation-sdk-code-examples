# frozen_string_literal: true

require 'minitest/autorun'
require 'securerandom'
require 'temporalio/testing'
require 'temporalio/worker'
require_relative '../parent_workflow'
require_relative '../abandon_parent_workflow'

# Registered under the name MyChildWorkflow so the parents start it instead of the real child. It runs until it gets a
# Signal, so a test can close the parent while the child is still running.
class LongRunningChildWorkflow < Temporalio::Workflow::Definition
  workflow_name :MyChildWorkflow

  def execute(_arg)
    Temporalio::Workflow.wait_condition { @finish }
  end

  workflow_signal
  def finish
    @finish = true
  end
end

class ChildWorkflowsTest < Minitest::Test
  def self.env
    @env ||= Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ).tap { |env| Minitest.after_run { env.shutdown } }
  end

  def client
    self.class.env.client
  end

  def with_worker(*workflows, &)
    @task_queue = "child-workflows-#{SecureRandom.uuid}"
    Temporalio::Worker.new(client:, task_queue: @task_queue, workflows:).run(&)
  end

  def start(workflow)
    client.start_workflow(workflow, id: "wf-#{SecureRandom.uuid}", task_queue: @task_queue)
  end

  # Waits for the parent to record that the child started, then returns a handle to the child.
  def started_child(parent)
    deadline = Time.now + 10
    loop do
      event = parent.fetch_history_events.find(&:child_workflow_execution_started_event_attributes)
      if event
        execution = event.child_workflow_execution_started_event_attributes.workflow_execution
        return client.workflow_handle(execution.workflow_id, run_id: execution.run_id)
      end
      raise 'Child Workflow did not start' if Time.now > deadline

      sleep 0.1
    end
  end

  def test_execute_child_workflow
    with_worker(ParentWorkflow, MyChildWorkflow) do
      assert_equal 'Child received my-workflow-arg', start(ParentWorkflow).result
    end
  end

  def test_default_policy_terminates_child
    with_worker(ParentWorkflow, LongRunningChildWorkflow) do
      parent = start(ParentWorkflow)
      child = started_child(parent)
      parent.terminate
      assert_raises(Temporalio::Error::WorkflowFailedError) { child.result }
      assert_equal Temporalio::Client::WorkflowExecutionStatus::TERMINATED, child.describe.status
    end
  end

  def test_abandon_policy_leaves_child_running
    with_worker(AbandonParentWorkflow, LongRunningChildWorkflow) do
      parent = start(AbandonParentWorkflow)
      child = started_child(parent)
      parent.terminate
      assert_equal Temporalio::Client::WorkflowExecutionStatus::TERMINATED, parent.describe.status
      assert_equal Temporalio::Client::WorkflowExecutionStatus::RUNNING, child.describe.status
      child.signal(LongRunningChildWorkflow.finish)
      child.result
      assert_equal Temporalio::Client::WorkflowExecutionStatus::COMPLETED, child.describe.status
    end
  end
end
