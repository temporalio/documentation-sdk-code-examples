# frozen_string_literal: true

require 'temporalio/client'
require 'temporalio/workflow'

# Defines its own MyWorkflow, so don't load it in the same process as the other MyWorkflow files in this folder.
class MyWorkflow < Temporalio::Workflow::Definition
  def execute(input)
    Temporalio::Workflow.wait_condition { @signal_input || @update_input }
    Temporalio::Workflow.wait_condition { Temporalio::Workflow.all_handlers_finished? }
    "#{input}, signal: #{@signal_input}, update: #{@update_input}"
  end

  workflow_signal
  def my_signal(input)
    @signal_input = input
  end

  workflow_update
  def my_update(input)
    @update_input = input
    "Received #{input}"
  end
end

def signal_with_start
  # @@@SNIPSTART ruby-message-passing-signal-with-start
  client = Temporalio::Client.connect('localhost:7233', 'default')

  # Create start-workflow operation for use with signal-with-start
  start_workflow_operation = Temporalio::Client::WithStartWorkflowOperation.new(
    MyWorkflow, 'my-workflow-input',
    id: 'my-workflow-id', task_queue: 'my-workflow-task-queue'
  )
  # Perform signal-with-start
  handle = client.signal_with_start_workflow(
    MyWorkflow.my_signal, 'signal-input', start_workflow_operation:
  )
  # @@@SNIPEND
  handle
end

def update_with_start
  # @@@SNIPSTART ruby-message-passing-update-with-start
  client = Temporalio::Client.connect('localhost:7233', 'default')

  # Create start-workflow operation for use with update-with-start
  start_workflow_operation = Temporalio::Client::WithStartWorkflowOperation.new(
    MyWorkflow, 'my-workflow-input',
    id: 'my-workflow-id', task_queue: 'my-workflow-task-queue',
    id_conflict_policy: Temporalio::WorkflowIDConflictPolicy::USE_EXISTING
  )
  # Perform update-with-start and get update result
  update_result = client.execute_update_with_start_workflow(
    MyWorkflow.my_update, 'update-input', start_workflow_operation:
  )
  # The workflow handle is on the start operation, here's an example of waiting on
  # workflow result
  workflow_result = start_workflow_operation.workflow_handle.result
  # @@@SNIPEND
  [update_result, workflow_result]
end
