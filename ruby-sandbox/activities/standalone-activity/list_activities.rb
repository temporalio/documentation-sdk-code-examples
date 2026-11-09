# frozen_string_literal: true

require 'temporalio/client'
require 'temporalio/env_config'

args, kwargs = Temporalio::EnvConfig::ClientConfig.load_client_connect_options
args[0] ||= 'localhost:7233'
args[1] ||= 'default'

client = Temporalio::Client.connect(*args, **kwargs)

# Only Standalone Activity Executions are returned. Activities scheduled inside
# Workflows are not.
# @@@SNIPSTART ruby-standalone-activity-list
client.list_activities("TaskQueue = 'standalone-activity-sample'").each do |execution|
  puts "#{execution.activity_id} #{execution.activity_type} #{execution.status}"
end
# @@@SNIPEND
