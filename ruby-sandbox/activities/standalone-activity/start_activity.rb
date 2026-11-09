# frozen_string_literal: true

require 'temporalio/client'
require 'temporalio/env_config'
require_relative 'my_activities'

args, kwargs = Temporalio::EnvConfig::ClientConfig.load_client_connect_options
args[0] ||= 'localhost:7233'
args[1] ||= 'default'

client = Temporalio::Client.connect(*args, **kwargs)

# @@@SNIPSTART ruby-standalone-activity-start
handle = client.start_activity(
  StandaloneActivity::MyActivities::ComposeGreeting,
  'Hello', 'World',
  id: 'standalone-activity-id',
  task_queue: 'standalone-activity-sample',
  start_to_close_timeout: 10
)
puts "Started Activity with id=#{handle.id} run_id=#{handle.run_id}"

# Wait for the result later
puts "Activity result: #{handle.result}"
# @@@SNIPEND
