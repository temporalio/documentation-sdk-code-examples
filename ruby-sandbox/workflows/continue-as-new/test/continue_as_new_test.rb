# frozen_string_literal: true

require 'minitest/autorun'
require 'securerandom'
require 'temporalio/testing'
require 'temporalio/worker'
require_relative '../continue_as_new_workflow'

class ContinueAsNewTest < Minitest::Test
  def self.env
    @env ||= Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ).tap { |env| Minitest.after_run { env.shutdown } }
  end

  def test_continue_as_new
    client = self.class.env.client
    task_queue = "continue-as-new-#{SecureRandom.uuid}"
    worker = Temporalio::Worker.new(client:, task_queue:, workflows: [ContinueAsNewWorkflow])
    worker.run do
      first_run = client.start_workflow(ContinueAsNewWorkflow, 'first-arg', id: "wf-#{SecureRandom.uuid}", task_queue:)

      # The result follows the chain to the run started by Continue-As-New.
      assert_equal 'Finished with my-new-arg', first_run.result

      # The start handle isn't bound to a run, so describe the first run by its Run ID.
      first = client.workflow_handle(first_run.id, run_id: first_run.result_run_id).describe
      assert_equal Temporalio::Client::WorkflowExecutionStatus::CONTINUED_AS_NEW, first.status

      # The new run has the same Workflow ID and a new Run ID.
      latest = client.workflow_handle(first_run.id).describe
      assert_equal first.id, latest.id
      refute_equal first.run_id, latest.run_id
      assert_equal Temporalio::Client::WorkflowExecutionStatus::COMPLETED, latest.status
    end
  end
end
