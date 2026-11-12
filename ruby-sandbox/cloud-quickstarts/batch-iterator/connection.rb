# frozen_string_literal: true

require 'temporalio/env_config'

module BatchIterator
  # Loads connection settings for the Worker and client.
  #
  # load_client_connect_options reads the TEMPORAL_PROFILE env var (default
  # "default"), loads that profile from the TOML config file (default OS config
  # dir, or TEMPORAL_CONFIG_FILE), and applies any TEMPORAL_* overrides. No
  # connection details are hardcoded, so the same Worker and client run against a
  # local dev server or Temporal Cloud.
  def self.connect_options
    args, kwargs = Temporalio::EnvConfig::ClientConfig.load_client_connect_options
    # With no config file and no env overrides the loaded options are empty, so
    # fall back to the local dev server.
    args[0] ||= 'localhost:7233'
    args[1] ||= 'default'
    [args, kwargs]
  end
end
