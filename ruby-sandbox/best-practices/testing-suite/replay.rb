# frozen_string_literal: true

require 'temporalio/worker/workflow_replayer'
require 'temporalio/workflow_history'
require_relative 'my_workflow'

# @@@SNIPSTART ruby-testing-replay-from-json
def replay_from_json(history_json)
  # Create a replayer
  replayer = Temporalio::Worker::WorkflowReplayer.new(workflows: [MyWorkflow])
  # Replay the history
  history = Temporalio::WorkflowHistory.from_history_json(history_json)
  replayer.replay_workflow(history)
end
# @@@SNIPEND

def replay_all(client)
  # @@@SNIPSTART ruby-testing-replay-all
  # Create a replayer
  replayer = Temporalio::Worker::WorkflowReplayer.new(workflows: [MyWorkflow])
  # Fetch the history of every MyWorkflow execution
  histories = client.list_workflows("WorkflowType = 'MyWorkflow'").map do |execution|
    client.workflow_handle(execution.id, run_id: execution.run_id).fetch_history
  end
  # Replay all the histories
  replayer.replay_workflows(histories).each do |result|
    # Raise if any failed (could have just set raise_on_replay_failure: true, but this
    # demonstrates iterating over the results)
    raise result.replay_failure if result.replay_failure
  end
  # @@@SNIPEND
end
