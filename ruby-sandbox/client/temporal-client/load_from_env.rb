# frozen_string_literal: true

# @@@SNIPSTART ruby-client-load-from-env
require 'temporalio/client'
require 'temporalio/env_config'

def main
  # load_client_connect_options is a helper that loads a profile and prepares
  # the configuration for Client.connect. By default, it loads the
  # "default" profile and also reads from environment variables. The environment
  # variables take precedence over the config file.
  args, kwargs = Temporalio::EnvConfig::ClientConfig.load_client_connect_options()

  puts "  Address: #{args[0]}"
  puts "  Namespace: #{args[1]}"
  puts "  gRPC Metadata: #{kwargs[:rpc_metadata]}"

  puts "\nAttempting to connect to client..."
  begin
    client = Temporalio::Client.connect(*args, **kwargs)
    puts '✅ Client connected successfully!'
    sys_info = client.workflow_service.get_system_info(Temporalio::Api::WorkflowService::V1::GetSystemInfoRequest.new)
    puts "✅ Successfully verified connection to Temporal server!\n#{sys_info}"
  rescue StandardError => e
    puts "❌ Failed to connect: #{e}"
  end
end

main if $PROGRAM_NAME == __FILE__
# @@@SNIPEND
