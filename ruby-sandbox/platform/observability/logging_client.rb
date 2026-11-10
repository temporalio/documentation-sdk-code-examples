# frozen_string_literal: true

# @@@SNIPSTART ruby-observability-client-logger
require 'logger'
require 'temporalio/client'

my_client = Temporalio::Client.connect(
  'localhost:7233', 'my-namespace',
  logger: Logger.new($stdout, level: Logger::INFO)
)
# @@@SNIPEND

puts "Connected to #{my_client.namespace} with logging"
