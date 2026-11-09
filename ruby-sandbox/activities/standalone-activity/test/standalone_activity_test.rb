# frozen_string_literal: true

require 'minitest/autorun'
require 'open3'
require 'rbconfig'
require 'temporalio/testing'

# Runs the page's programs as separate processes, the way the quickstart does,
# against a local dev server.
class StandaloneActivityTest < Minitest::Test
  DIR = File.expand_path('..', __dir__)

  def test_quickstart
    Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ) do |env|
      address = env.client.connection.target_host
      process_env = { 'TEMPORAL_ADDRESS' => address }
      worker = spawn(process_env, RbConfig.ruby, File.join(DIR, 'worker.rb'), out: File::NULL)
      begin
        out, status = Open3.capture2e(process_env, RbConfig.ruby, File.join(DIR, 'execute_activity.rb'))
        assert status.success?, out
        assert_equal "Activity result: Hello, World!\n", out

        # The page's Temporal CLI alternative runs the same Activity.
        cli = ENV.fetch('TEMPORAL_CLI_PATH', 'temporal')
        out, status = Open3.capture2e(
          cli, 'activity', 'execute', '--address', address,
          '--type', 'ComposeGreeting', '--activity-id', 'standalone-activity-id',
          '--task-queue', 'standalone-activity-sample', '--start-to-close-timeout', '10s',
          '--input', '"Hello"', '--input', '"World"'
        )
        assert status.success?, out
        assert_includes out, 'Hello, World!'
      ensure
        Process.kill('INT', worker)
        Process.wait(worker)
      end
    end
  end
end
