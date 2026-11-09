# frozen_string_literal: true

require 'temporalio/client'
require 'temporalio/worker'
require_relative 'cutover_workflows'

def build_cutover_worker
  # @@@SNIPSTART ruby-versioning-cutover-worker
  client = Temporalio::Client.connect('localhost:7233', 'default')

  worker = Temporalio::Worker.new(
    client:,
    task_queue: 'my-task-queue',
    workflows: [MyWorkflow, MyWorkflowV2]
  )
  # @@@SNIPEND
  worker
end
