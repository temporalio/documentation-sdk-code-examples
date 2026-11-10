# frozen_string_literal: true

require 'minitest/autorun'
require 'open3'
require 'rbconfig'
require 'securerandom'
require 'temporalio/testing'
require_relative '../worker'

class RunWorkerTest < Minitest::Test
  DIR = File.expand_path('..', __dir__)
  CLI = ENV.fetch('TEMPORAL_CLI_PATH', 'temporal')

  def with_env(&)
    Temporalio::Testing::WorkflowEnvironment.start_local(dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil), &)
  end

  # Runs Ruby code that loads worker.rb and connects to the test server, in a
  # child process so the test can send it signals.
  def spawn_ruby(env, code)
    script = <<~RUBY
      require #{File.join(DIR, 'worker').inspect}
      $stdout.sync = true
      client = Temporalio::Client.connect(#{env.client.connection.target_host.inspect}, 'default')
      #{code}
    RUBY
    out_r, out_w = IO.pipe
    pid = spawn(RbConfig.ruby, '-e', script, out: out_w, err: out_w)
    out_w.close
    [pid, out_r]
  end

  def run_wf(env, wf, arg, task_queue: 'my-task-queue')
    env.client.execute_workflow(wf, arg, id: "wf-#{SecureRandom.uuid}", task_queue:)
  end

  def test_create_and_run_worker
    with_env do |env|
      pid, = spawn_ruby(env, 'run_worker(client)')
      assert_equal 'Hello, Temporal!', run_wf(env, GreetingWorkflow, 'Temporal')
    ensure
      Process.kill('KILL', pid) if pid
      Process.wait(pid) if pid
    end
  end

  def test_register_class_and_instance
    payment_client = Struct.new(:charges) do
      def charge(amount)
        charges << amount
        "charged #{amount}"
      end
    end.new([])
    with_env do |env|
      build_worker_with_types(env.client, payment_client).run do
        assert_equal 'Hello, Temporal!', run_wf(env, GreetingWorkflow, 'Temporal')
        assert_equal 'charged 5', run_wf(env, OrderWorkflow, { amount: 5 })
        assert_equal 'charged 7', run_wf(env, OrderWorkflow, { amount: 7 })
      end
      # Both executions used the one ChargeCard instance and its payment client.
      assert_equal [5, 7], payment_client.charges
    end
  end

  def test_versioned_worker
    with_env do |env|
      address = env.client.connection.target_host
      build_versioned_worker(env.client).run do
        # Make 1.0 the current version once the Worker has registered it.
        deadline = Time.now + 20
        loop do
          out, status = Open3.capture2e(CLI, 'worker', 'deployment', 'set-current-version', '--address', address,
                                        '--deployment-name', 'my-app', '--build-id', '1.0', '--yes')
          break if status.success?
          raise out if Time.now > deadline

          sleep 0.5
        end
        handle = env.client.start_workflow(VersionedGreetingWorkflow, 'Temporal', id: "wf-#{SecureRandom.uuid}",
                                                                                  task_queue: 'my-task-queue')
        assert_equal 'Hello, Temporal!', handle.result
        info = handle.describe.raw_description.workflow_execution_info.versioning_info
        assert_equal :VERSIONING_BEHAVIOR_PINNED, info.behavior
        assert_equal 'my-app', info.deployment_version.deployment_name
        assert_equal '1.0', info.deployment_version.build_id
      end
    end
  end

  def test_shutdown_signals
    with_env do |env|
      %w[SIGINT SIGTERM].each do |signal|
        pid, out = spawn_ruby(env, <<~RUBY)
          worker = Temporalio::Worker.new(client:, task_queue: 'my-task-queue', workflows: [GreetingWorkflow], activities: [SayHello])
          puts 'running'
          run_until_signal(worker)
          puts 'stopped'
        RUBY
        assert_equal "running\n", out.gets
        assert_equal 'Hello, Temporal!', run_wf(env, GreetingWorkflow, 'Temporal')
        Process.kill(signal.delete_prefix('SIG'), pid)
        _, status = Process.wait2(pid)
        assert status.success?, "exit status #{status.inspect} after #{signal}"
        assert_equal "stopped\n", out.gets
      end
    end
  end
end
