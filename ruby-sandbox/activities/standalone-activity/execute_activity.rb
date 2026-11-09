# frozen_string_literal: true

# @@@SNIPSTART ruby-standalone-activity-quickstart-execute
require 'temporalio/client'
require 'temporalio/env_config'
require_relative 'my_activities'

args, kwargs = Temporalio::EnvConfig::ClientConfig.load_client_connect_options
args[0] ||= 'localhost:7233'
args[1] ||= 'default'

client = Temporalio::Client.connect(*args, **kwargs)

result = client.execute_activity(
  StandaloneActivity::MyActivities::ComposeGreeting,
  'Hello', 'World',
  id: 'standalone-activity-id',
  task_queue: 'standalone-activity-sample',
  start_to_close_timeout: 10
)
puts "Activity result: #{result}"
# @@@SNIPEND
