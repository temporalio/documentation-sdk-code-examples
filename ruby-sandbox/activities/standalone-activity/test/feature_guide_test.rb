# frozen_string_literal: true

require 'minitest/autorun'
require 'open3'
require 'rbconfig'
require 'temporalio/testing'
require_relative '../activity_handle'
require_relative '../my_activities'

# Runs the feature guide's programs and Temporal CLI commands as separate
# processes against a local dev server, with worker.rb running.
class FeatureGuideTest < Minitest::Test
  DIR = File.expand_path('..', __dir__)
  ID = 'standalone-activity-id'

  def run_program(file)
    out, status = Open3.capture2e(@process_env, RbConfig.ruby, File.join(DIR, file))
    assert status.success?, out
    out
  end

  def run_cli(*args)
    cli = ENV.fetch('TEMPORAL_CLI_PATH', 'temporal')
    out, status = Open3.capture2e(cli, 'activity', *args, '--address', @address)
    assert status.success?, out
    out
  end

  def eventually(timeout: 10)
    deadline = Time.now + timeout
    loop do
      return yield
    rescue Minitest::Assertion
      raise if Time.now > deadline

      sleep 0.2
    end
  end

  def test_feature_guide
    Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ) do |env|
      client = env.client
      @address = client.connection.target_host
      @process_env = { 'TEMPORAL_ADDRESS' => @address }
      worker = spawn(@process_env, RbConfig.ruby, File.join(DIR, 'worker.rb'), out: File::NULL)
      begin
        # Start without waiting, then wait on the handle.
        out = run_program('start_activity.rb')
        assert_match(/\AStarted Activity with id=#{ID} run_id=\S+\nActivity result: Hello, World!\n\z/, out)

        # The CLI alternatives: start, then wait for the result by Activity ID.
        run_cli('start', '--type', 'ComposeGreeting', '--activity-id', ID,
                '--task-queue', 'standalone-activity-sample', '--start-to-close-timeout', '10s',
                '--input', '"Hello"', '--input', '"World"')
        assert_includes run_cli('result', '--activity-id', ID), 'Hello, World!'

        # A handle by ID targets the latest run.
        handle = get_activity_handle(client)
        assert_equal 'Hello, World!', wait_for_result(handle)
        assert_equal Temporalio::Client::ActivityExecutionStatus::COMPLETED, handle.describe.status

        # Both runs are listed and counted.
        eventually do
          lines = run_program('list_activities.rb').lines
          assert_equal 2, lines.size
          lines.each { |l| assert_match(/\A#{ID} ComposeGreeting #{Temporalio::Client::ActivityExecutionStatus::COMPLETED}\n\z/, l) }
        end
        eventually { assert_equal "Total: 2\n", run_program('count_activities.rb') }
        assert_includes run_cli('list'), ID
        assert_includes run_cli('count'), '2'
      ensure
        Process.kill('INT', worker)
        Process.wait(worker)
      end

      # cancel and terminate each close a running Activity. No Worker polls this
      # Task Queue, so the Activity stays running until then.
      start = lambda do |id|
        client.start_activity('ComposeGreeting', 'Hello', 'World', id:, task_queue: 'no-worker',
                                                                    start_to_close_timeout: 10)
      end
      to_cancel = start.call("#{ID}-cancel")
      assert_equal Temporalio::Client::ActivityExecutionStatus::RUNNING, to_cancel.describe.status
      to_cancel.cancel
      assert_equal Temporalio::Client::ActivityExecutionStatus::CANCELED, to_cancel.describe.status

      to_terminate = start.call("#{ID}-terminate")
      to_terminate.terminate
      assert_equal Temporalio::Client::ActivityExecutionStatus::TERMINATED, to_terminate.describe.status
    end
  end
end
