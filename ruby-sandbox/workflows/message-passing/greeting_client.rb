# frozen_string_literal: true

require 'temporalio/client'
require_relative 'greeting_workflow'

def start_greeting_workflow
  # @@@SNIPSTART ruby-message-passing-start-workflow
  client = Temporalio::Client.connect('localhost:7233', 'default')
  handle = client.start_workflow(
    GreetingWorkflow,
    id: 'message-passing-simple-sample-workflow-id',
    task_queue: 'message-passing-simple-sample'
  )
  # @@@SNIPEND
  handle
end

def query_languages(handle)
  # @@@SNIPSTART ruby-message-passing-send-query
  supported_languages = handle.query(GreetingWorkflow.languages, { include_unsupported: false })
  # @@@SNIPEND
  supported_languages
end

def send_approve_signal(handle)
  # @@@SNIPSTART ruby-message-passing-send-signal
  handle.signal(GreetingWorkflow.approve, { name: 'John Q. Approver' })
  # @@@SNIPEND
end

def execute_set_language_update(handle)
  # @@@SNIPSTART ruby-message-passing-execute-update
  prev_language = handle.execute_update(GreetingWorkflow.set_language, :chinese)
  # @@@SNIPEND
  prev_language
end

def start_apply_language_update(handle)
  # @@@SNIPSTART ruby-message-passing-start-update
  # Start an update and then wait for it to complete
  update_handle = handle.start_update(
    GreetingWorkflow.apply_language_with_lookup,
    :arabic,
    wait_for_stage: Temporalio::Client::WorkflowUpdateWaitStage::ACCEPTED
  )
  prev_language = update_handle.result
  # @@@SNIPEND
  prev_language
end
