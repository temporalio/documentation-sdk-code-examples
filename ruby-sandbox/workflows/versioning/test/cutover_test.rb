# frozen_string_literal: true

require 'minitest/autorun'
require 'minitest/mock'
require 'securerandom'
require 'temporalio/testing'
require_relative '../cutover_worker'

class CutoverTest < Minitest::Test
  def test_worker_runs_both_workflow_types
    Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ) do |env|
      # The page's Worker connects to localhost:7233; point it at the test server instead.
      worker = Temporalio::Client.stub(:connect, env.client) { build_cutover_worker }
      worker.run do
        [MyWorkflow, MyWorkflowV2].each do |workflow|
          handle = env.client.start_workflow(workflow, id: "wf-#{SecureRandom.uuid}", task_queue: 'my-task-queue')
          assert_nil handle.result
        end
      end
    end
  end
end
