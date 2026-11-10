# frozen_string_literal: true

# @@@SNIPSTART ruby-cloud-run-worker
require 'temporalio/client'
require 'temporalio/worker'
require_relative 'greeting_workflow'

client = Temporalio::Client.connect(
  ENV.fetch('TEMPORAL_ADDRESS'),
  ENV.fetch('TEMPORAL_NAMESPACE'),
  api_key: ENV.fetch('TEMPORAL_API_KEY'),
  tls: true
)

worker = Temporalio::Worker.new(
  client:,
  task_queue: ENV.fetch('TEMPORAL_TASK_QUEUE'),
  workflows: [GreetingWorkflow],
  activities: [SayHello],
  deployment_options: Temporalio::Worker::DeploymentOptions.new(
    version: Temporalio::WorkerDeploymentVersion.new(
      deployment_name: 'my-app',
      build_id: 'build-1'
    ),
    use_worker_versioning: true,
    default_versioning_behavior: Temporalio::VersioningBehavior::PINNED
  )
)

worker.run
# @@@SNIPEND
