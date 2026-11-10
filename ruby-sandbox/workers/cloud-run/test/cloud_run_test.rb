# frozen_string_literal: true

require 'minitest/autorun'
require 'open3'
require 'rbconfig'
require 'securerandom'
require 'temporalio/testing'
require 'temporalio/worker'
require_relative '../greeting_workflow'
require_relative '../per_workflow_behavior'
require_relative '../process_items_activity'

class CloudRunTest < Minitest::Test
  DIR = File.expand_path('..', __dir__)
  CLI = ENV.fetch('TEMPORAL_CLI_PATH', 'temporal')

  def with_env(&)
    Temporalio::Testing::WorkflowEnvironment.start_local(dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil), &)
  end

  def set_current_version(address, build_id)
    deadline = Time.now + 20
    loop do
      out, status = Open3.capture2e(CLI, 'worker', 'deployment', 'set-current-version', '--address', address,
                                    '--deployment-name', 'my-app', '--build-id', build_id, '--yes')
      return if status.success?
      raise out if Time.now > deadline

      sleep 0.5
    end
  end

  # Runs worker.rb as the container would, with Client.connect redirected to the
  # local dev server. The child prints the connection arguments it was given.
  def test_worker_reads_environment_and_runs_pinned
    with_env do |env|
      address = env.client.connection.target_host
      task_queue = "cloud-run-#{SecureRandom.uuid}"
      script = <<~RUBY
        require 'temporalio/client'
        connect = Temporalio::Client.method(:connect)
        Temporalio::Client.define_singleton_method(:connect) do |target, namespace, **kwargs|
          puts [target, namespace, kwargs[:api_key], kwargs[:tls]].inspect
          $stdout.flush
          connect.call(#{address.inspect}, namespace)
        end
        load #{File.join(DIR, 'worker.rb').inspect}
      RUBY
      child_env = { 'TEMPORAL_ADDRESS' => 'my-ns.a1b2c.tmprl.cloud:7233', 'TEMPORAL_NAMESPACE' => 'default',
                    'TEMPORAL_API_KEY' => 'my-api-key', 'TEMPORAL_TASK_QUEUE' => task_queue }
      out_r, out_w = IO.pipe
      pid = spawn(child_env, RbConfig.ruby, '-e', script, out: out_w, err: out_w)
      out_w.close
      begin
        assert_equal '["my-ns.a1b2c.tmprl.cloud:7233", "default", "my-api-key", true]', out_r.gets&.chomp
        set_current_version(address, 'build-1')
        handle = env.client.start_workflow(GreetingWorkflow, 'Temporal', id: "wf-#{SecureRandom.uuid}", task_queue:)
        assert_equal 'Hello, Temporal!', handle.result
        # default_versioning_behavior pinned the Workflow, which sets none itself.
        info = handle.describe.raw_description.workflow_execution_info.versioning_info
        assert_equal :VERSIONING_BEHAVIOR_PINNED, info.behavior
        assert_equal 'build-1', info.deployment_version.build_id
      ensure
        Process.kill('KILL', pid)
        Process.wait(pid)
      end
    end
  end

  def test_per_workflow_behavior
    info = Temporalio::Workflow::Definition::Info.from_class(PerWorkflowBehavior::GreetingWorkflow)
    assert_equal Temporalio::VersioningBehavior::PINNED, info.versioning_behavior
  end

  def test_missing_behavior_fails_at_startup
    with_env do |env|
      err = assert_raises(StandardError) do
        Temporalio::Worker.new(
          client: env.client, task_queue: 'unused', workflows: [GreetingWorkflow], activities: [SayHello],
          deployment_options: Temporalio::Worker::DeploymentOptions.new(
            version: Temporalio::WorkerDeploymentVersion.new(deployment_name: 'my-app', build_id: 'build-1'),
            use_worker_versioning: true
          )
        )
      end
      puts "startup error: #{err.class}: #{err.message}"
    end
  end

  def test_heartbeat_resumes_from_last_item
    converter = Temporalio::Converters::PayloadConverter.default
    run = lambda do |details|
      beats = []
      info = Temporalio::Testing::ActivityEnvironment.default_info.with(
        raw_heartbeat_details: details.map { |d| Temporalio::Converters::RawValue.new(converter.to_payload(d)) }
      )
      env = Temporalio::Testing::ActivityEnvironment.new(info:, on_heartbeat: ->(d) { beats << d.first })
      [env.run(ProcessItems, %w[a b c d]), beats]
    end
    assert_equal ['done', [0, 1, 2, 3]], run.call([])
    # A retry after the last attempt recorded item 2 starts again at item 2.
    assert_equal ['done', [2, 3]], run.call([2])
  end
end
