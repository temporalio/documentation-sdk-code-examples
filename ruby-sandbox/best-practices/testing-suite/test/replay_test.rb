# frozen_string_literal: true

require 'minitest/autorun'
require 'securerandom'
require 'temporalio/testing'
require 'temporalio/worker'
require_relative '../replay'

# Changes MyWorkflow's commands so replaying old histories fails.
class ChangedMyWorkflow < Temporalio::Workflow::Definition
  workflow_name :MyWorkflow

  def execute(name)
    "Hello, #{name}!"
  end
end

class ReplayTest < Minitest::Test
  def test_replay
    Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ) do |env|
      client = env.client
      worker = Temporalio::Worker.new(client:, task_queue: "tq-#{SecureRandom.uuid}", workflows: [MyWorkflow])
      handles = worker.run do
        2.times.map do |i|
          handle = client.start_workflow(MyWorkflow, "name-#{i}",
                                         id: "wf-#{SecureRandom.uuid}", task_queue: worker.task_queue)
          handle.result
          handle
        end
      end

      history_json = handles.first.fetch_history.to_history_json
      replay_from_json(history_json)

      # Visibility is eventually consistent
      sleep 0.2 until client.count_workflows("WorkflowType = 'MyWorkflow'").count == 2
      replay_all(client)

      # The same code rejects histories that don't match the Workflow code
      changed = Temporalio::Worker::WorkflowReplayer.new(workflows: [ChangedMyWorkflow])
      assert_raises(Temporalio::Workflow::NondeterminismError) do
        changed.replay_workflow(Temporalio::WorkflowHistory.from_history_json(history_json))
      end
    end
  end
end
