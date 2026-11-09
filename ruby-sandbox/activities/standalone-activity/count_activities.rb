# frozen_string_literal: true

require 'temporalio/client'
require 'temporalio/env_config'

args, kwargs = Temporalio::EnvConfig::ClientConfig.load_client_connect_options
args[0] ||= 'localhost:7233'
args[1] ||= 'default'

client = Temporalio::Client.connect(*args, **kwargs)

# Only Standalone Activity Executions are counted. Activities scheduled inside
# Workflows are not.
# @@@SNIPSTART ruby-standalone-activity-count
result = client.count_activities("TaskQueue = 'standalone-activity-sample'")
puts "Total: #{result.count}"
result.groups.each do |group|
  puts "  #{group.group_values.join(',')} => #{group.count}"
end
# @@@SNIPEND
