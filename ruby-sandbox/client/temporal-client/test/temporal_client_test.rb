# frozen_string_literal: true

require 'minitest/autorun'
require 'minitest/mock'
require 'open3'
require 'rbconfig'
require 'securerandom'
require 'tmpdir'
require 'temporalio/client'
require 'temporalio/env_config'
require 'temporalio/worker'
require_relative '../connect'
require_relative '../start_workflow'

# The page's code connects to localhost:7233, so the test runs a dev server on
# that port. It listens on both IPv4 and IPv6 because localhost can resolve to
# either one.
class TemporalClientTest < Minitest::Test
  DIR = File.expand_path('..', __dir__)
  Env = Struct.new(:client)

  def self.env
    @env ||= begin
      cli = ENV.fetch('TEMPORAL_CLI_PATH', 'temporal')
      pid = spawn(cli, 'server', 'start-dev', '--headless', '--ip', '::', '--port', '7233', out: File::NULL, err: File::NULL)
      Minitest.after_run do
        Process.kill('TERM', pid)
        Process.wait(pid)
      end
      deadline = Time.now + 30
      client = begin
        Temporalio::Client.connect('127.0.0.1:7233', 'default')
      rescue Temporalio::Error::RPCError, Temporalio::Internal::Bridge::Error
        raise if Time.now > deadline

        sleep 0.3
        retry
      end
      Env.new(client)
    end
  end

  def env = self.class.env

  # Every test needs the server, including those that only run the page's programs.
  def setup = env

  # Runs a program with an empty HOME, so no user config file is read.
  def run_program(file, extra_env = {})
    Dir.mktmpdir do |home|
      out, status = Open3.capture2e({ 'HOME' => home }.merge(extra_env), RbConfig.ruby, File.join(DIR, file))
      assert status.success?, out
      out
    end
  end

  def load_profile(file, profile)
    Temporalio::EnvConfig::ClientConfig.load_client_connect_options(
      profile:, config_source: Pathname.new(File.join(DIR, file))
    )
  end

  def test_load_from_file
    out = run_program('load_from_file.rb')
    assert_includes out, 'Address: localhost:7233'
    assert_includes out, '"my-custom-header" => "development-value"'
    assert_includes out, '"trace-id" => "dev-trace-123"'
    assert_includes out, '✅ Successfully verified connection to Temporal server!'
  end

  def test_load_from_env
    out = run_program('load_from_env.rb', 'TEMPORAL_ADDRESS' => 'localhost:7233', 'TEMPORAL_NAMESPACE' => 'default')
    assert_includes out, 'Address: localhost:7233'
    assert_includes out, '✅ Successfully verified connection to Temporal server!'

    # Environment variables take precedence over the config file.
    Dir.mktmpdir do |dir|
      File.write(File.join(dir, 'temporal.toml'), "[profile.default]\naddress = \"localhost:9999\"\n")
      out = run_program('load_from_env.rb', 'TEMPORAL_CONFIG_FILE' => File.join(dir, 'temporal.toml'),
                                            'TEMPORAL_ADDRESS' => 'localhost:7233')
      assert_includes out, 'Address: localhost:7233'
    end
  end

  def test_load_profile_with_override
    out = run_program('load_profile.rb')
    assert_includes out, 'Address: localhost:7233 (overridden from localhost:9999)'
    assert_includes out, 'Namespace: staging'
    assert_includes out, '✅ Successfully verified connection to Temporal server!'
    assert_equal 'localhost:9999', load_profile('config.toml', 'staging')[0][0]
  end

  def test_config_file_prod_profile
    Dir.mktmpdir do |dir|
      # Point the prod profile's certificate paths at files that exist.
      %w[client.pem client.key].each { |f| File.write(File.join(dir, f), f) }
      toml = File.read(File.join(DIR, 'config.toml')).gsub('/etc/temporal/certs', dir)
      File.write(File.join(dir, 'config.toml'), toml)
      args, kwargs = Temporalio::EnvConfig::ClientConfig.load_client_connect_options(
        profile: 'prod', config_source: Pathname.new(File.join(dir, 'config.toml'))
      )
      assert_equal ['your-namespace.a1b2c.tmprl.cloud:7233', 'your-namespace'], args
      assert_equal 'your-api-key-here', kwargs[:api_key]
      assert_equal 'client.pem', kwargs[:tls].client_cert
      assert_equal({ 'environment' => 'production', 'service-version' => 'v1.2.3' }, kwargs[:rpc_metadata])
    end
  end

  def test_cloud_config_files
    args, kwargs = load_profile('cloud_api_key.toml', 'staging')
    assert_equal ['your-namespace.a1b2c.tmprl.cloud:7233', 'your-namespace'], args
    assert_equal 'your-api-key-here', kwargs[:api_key]
    assert kwargs[:tls], 'an API key turns TLS on'

    Dir.mktmpdir do |dir|
      key = File.join(dir, 'client.key')
      File.write(key, 'my-key')
      File.write(File.join(dir, 'c.toml'), File.read(File.join(DIR, 'cloud_mtls.toml')).sub('your-tls-client-key-path', key))
      _, kwargs = Temporalio::EnvConfig::ClientConfig.load_client_connect_options(
        profile: 'staging', config_source: Pathname.new(File.join(dir, 'c.toml'))
      )
      assert_equal 'your-tls-client-cert-data', kwargs[:tls].client_cert
      assert_equal 'my-key', kwargs[:tls].client_private_key
    end
  end

  def test_connect_local
    assert_equal 'default', connect_local.namespace
  end

  def test_connect_cloud
    calls = []
    fake_connect = ->(*args, **kwargs) { calls << [args, kwargs] }
    Dir.mktmpdir do |dir|
      Dir.chdir(dir) do
        %w[my-client-cert.pem my-client-key.pem].each { |f| File.write(f, f) }
        Temporalio::Client.stub(:connect, fake_connect) do
          connect_cloud_api_key
          connect_cloud_mtls
        end
      end
    end
    assert_equal [['<endpoint>', '<namespace_id>.<account_id>'], { api_key: '<api_key>', tls: true }], calls[0]
    tls = calls[1][1][:tls]
    assert_equal ['my-client-cert.pem', 'my-client-key.pem'], [tls.client_cert, tls.client_private_key]
  end

  def test_rotate_client_certificate
    new_client = Temporalio::Client.connect(env.client.connection.target_host, 'default')
    task_queue = "rotate-#{SecureRandom.uuid}"
    worker = Temporalio::Worker.new(client: env.client, task_queue:, workflows: [MyWorkflow])
    worker.run do
      Dir.mktmpdir do |dir|
        Dir.chdir(dir) do
          %w[my-client-cert-new.pem my-client-key-new.pem].each { |f| File.write(f, f) }
          Temporalio::Client.stub(:connect, new_client) { rotate_client_certificate(worker) }
        end
      end
      assert_same new_client, worker.client
      # The running Worker keeps processing Tasks with the new client.
      result = env.client.execute_workflow(MyWorkflow, 'x', id: "wf-#{SecureRandom.uuid}", task_queue:)
      assert_equal 'Received x', result
    end
  end

  def test_start_workflow_and_get_result
    Temporalio::Worker.new(client: env.client, task_queue: 'my-task-queue', workflows: [MyWorkflow]).run do
      assert_output("Result: Received some-input\n") { execute_my_workflow(env.client) }
      # Workflow ID reuse allows a new run after the first one closes.
      assert_equal 'Received some-input', execute_with_options(env.client)
      assert_output("Result: Received some-input\n") { get_workflow_result(env.client) }
      desc = env.client.workflow_handle('my-workflow-id').describe
      assert_equal 'my-task-queue', desc.task_queue
    end
  end
end
