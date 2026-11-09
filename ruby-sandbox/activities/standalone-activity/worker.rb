# frozen_string_literal: true

# @@@SNIPSTART ruby-standalone-activity-quickstart-worker
require 'temporalio/client'
require 'temporalio/env_config'
require 'temporalio/worker'
require_relative 'my_activities'

args, kwargs = Temporalio::EnvConfig::ClientConfig.load_client_connect_options
args[0] ||= 'localhost:7233'
args[1] ||= 'default'

client = Temporalio::Client.connect(*args, **kwargs)

# A Worker for Standalone Activities is configured the same way as one for
# Workflow Activities. No Workflows are required.
worker = Temporalio::Worker.new(
  client:,
  task_queue: 'standalone-activity-sample',
  activities: [StandaloneActivity::MyActivities::ComposeGreeting]
)

puts 'Starting worker (ctrl+c to exit)'
worker.run(shutdown_signals: ['SIGINT'])
# @@@SNIPEND
