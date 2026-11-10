# frozen_string_literal: true

require 'temporalio/client'
require 'temporalio/worker'
require 'temporalio/worker_deployment_version'
require_relative 'workflows'

def run_worker(client)
  # @@@SNIPSTART ruby-create-worker
  worker = Temporalio::Worker.new(
    client: client,
    task_queue: 'my-task-queue',
    workflows: [GreetingWorkflow],
    activities: [SayHello]
  )

  worker.run
  # @@@SNIPEND
end

def build_worker_with_types(client, payment_client)
  # @@@SNIPSTART ruby-worker-register-types
  worker = Temporalio::Worker.new(
    client:,
    task_queue: 'my-task-queue',
    workflows: [GreetingWorkflow, OrderWorkflow],
    activities: [SayHello, ChargeCard.new(payment_client)]
  )
  # @@@SNIPEND
  worker
end

def build_versioned_worker(client)
  # @@@SNIPSTART ruby-versioned-worker
  worker = Temporalio::Worker.new(
    client: client,
    task_queue: 'my-task-queue',
    workflows: [VersionedGreetingWorkflow],
    activities: [SayHello],
    deployment_options: Temporalio::Worker::DeploymentOptions.new(
      version: Temporalio::WorkerDeploymentVersion.new(
        deployment_name: 'my-app',
        build_id: '1.0'
      ),
      use_worker_versioning: true
    )
  )
  # @@@SNIPEND
  worker
end

def run_until_signal(worker)
  # @@@SNIPSTART ruby-worker-graceful-shutdown
  worker.run(shutdown_signals: %w[SIGINT SIGTERM])
  # @@@SNIPEND
end
