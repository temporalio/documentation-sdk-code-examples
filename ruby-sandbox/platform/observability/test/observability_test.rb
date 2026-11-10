# frozen_string_literal: true

require 'logger'
require 'minitest/autorun'
require 'minitest/mock'
require 'net/http'
require 'open3'
require 'rbconfig'
require 'securerandom'
require 'stringio'
require 'temporalio/client'
require 'temporalio/testing'
require 'temporalio/worker'
require_relative '../list_workflows'
require_relative '../logging_workflow'
require_relative '../upsert_workflow'

class GreetingWorkflow < Temporalio::Workflow::Definition
  def execute = 'hello'
end

class ObservabilityTest < Minitest::Test
  DIR = File.expand_path('..', __dir__)
  ROOT = File.expand_path('../../..', __dir__)

  def self.env
    @env ||= Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil),
      dev_server_extra_args: ['--search-attribute', 'my-keyword=Keyword']
    ).tap { |e| Minitest.after_run { e.shutdown } }
  end

  def env = self.class.env
  def address = env.client.connection.target_host

  def eventually(timeout: 10)
    deadline = Time.now + timeout
    begin
      yield
    rescue Minitest::Assertion
      raise if Time.now > deadline

      sleep 0.2
      retry
    end
  end

  # Runs setup code that configures the runtime in a child process, makes a
  # client call, then returns the Prometheus scrape from that process.
  def scrape_metrics(setup, port:)
    script = <<~RUBY
      require 'temporalio/client'
      require 'temporalio/runtime'
      #{setup}
      client = Temporalio::Client.connect(#{address.inspect}, 'default')
      client.list_workflows.first
      puts 'ready'
      $stdout.flush
      sleep
    RUBY
    out_r, out_w = IO.pipe
    pid = spawn(RbConfig.ruby, '-e', script, out: out_w)
    out_w.close
    assert_equal "ready\n", out_r.gets
    Net::HTTP.get(URI("http://127.0.0.1:#{port}/metrics"))
  ensure
    Process.kill('KILL', pid) if pid
    Process.wait(pid) if pid
  end

  def test_prometheus_endpoint
    body = scrape_metrics("require #{File.join(DIR, 'prometheus_metrics').inspect}\nconfigure_prometheus_metrics", port: 9000)
    assert_match(/^temporal_request\{/, body)
  end

  def test_global_tags
    # The page's OpenTelemetry configuration loads without error.
    script = "require #{File.join(ROOT, 'telemetry-global-tags').inspect}; OpenTelemetry::Util.configure_metrics_and_tracing"
    out, status = Open3.capture2e(RbConfig.ruby, '-e', script)
    assert status.success?, out

    # global_tags reach the metrics an exporter emits. Prometheus shows them
    # without running an OpenTelemetry collector.
    body = scrape_metrics(<<~RUBY, port: 9001)
      Temporalio::Runtime.default = Temporalio::Runtime.new(
        telemetry: Temporalio::Runtime::TelemetryOptions.new(
          metrics: Temporalio::Runtime::MetricsOptions.new(
            prometheus: Temporalio::Runtime::PrometheusMetricsOptions.new(bind_address: '127.0.0.1:9001'),
            global_tags: { 'team' => 'content-platform', 'environment' => 'production' }
          )
        )
      )
    RUBY
    request_lines = body.lines.grep(/^temporal_request\{/)
    refute_empty request_lines
    assert(request_lines.all? { |l| l.include?('team="content-platform"') && l.include?('environment="production"') })
  end

  # Loads a page script with Client.connect replaced, and returns the arguments it got.
  def load_client_script(file)
    calls = []
    fake = Struct.new(:namespace).new('my-namespace')
    Temporalio::Client.stub(:connect, lambda { |*args, **kwargs|
      calls << [args, kwargs]
      fake
    }) do
      assert_output(/Connected to my-namespace/) { load File.join(DIR, file) }
    end
    calls.first
  end

  def test_tracing_client
    args, kwargs = load_client_script('tracing_client.rb')
    assert_equal ['localhost:7233', 'my-namespace'], args
    assert_instance_of Temporalio::Contrib::OpenTelemetry::TracingInterceptor, kwargs[:interceptors].first
  end

  def test_logging_client
    args, kwargs = load_client_script('logging_client.rb')
    assert_equal ['localhost:7233', 'my-namespace'], args
    assert_equal Logger::INFO, kwargs[:logger].level
  end

  def test_workflow_and_activity_loggers
    log = StringIO.new
    task_queue = "logging-#{SecureRandom.uuid}"
    Temporalio::Worker.new(
      client: env.client, task_queue:, workflows: [LoggingWorkflow], activities: [LoggingActivity],
      logger: Logger.new(log, level: Logger::INFO)
    ).run do
      env.client.execute_workflow(LoggingWorkflow, 'abc', id: "wf-#{SecureRandom.uuid}", task_queue:)
    end
    lines = log.string.lines.grep(/Some log abc/)
    # The Workflow logger adds Workflow details; the Activity logger adds Activity details.
    assert(lines.any? { |l| l.include?('workflow_type: "LoggingWorkflow"') && !l.include?('activity_type') }, log.string)
    assert(lines.any? { |l| l.include?('activity_type: "LoggingActivity"') }, log.string)
  end

  def test_list_workflows
    task_queue = "list-#{SecureRandom.uuid}"
    Temporalio::Worker.new(client: env.client, task_queue:, workflows: [GreetingWorkflow]).run do
      2.times { |i| env.client.execute_workflow(GreetingWorkflow, id: "greeting-#{i}", task_queue:) }
    end
    eventually do
      out, = capture_io { list_greeting_workflows(env.client) }
      assert_equal ['Workflow: greeting-0', 'Workflow: greeting-1'], out.lines.map(&:chomp).sort
    end
  end

  def test_set_and_upsert_search_attributes
    Temporalio::Worker.new(client: env.client, task_queue: 'my-task-queue', workflows: [MyWorkflow]).run do
      out, status = Open3.capture2e({ 'TEMPORAL_ADDRESS' => address }, RbConfig.ruby,
                                    File.join(DIR, 'start_with_search_attributes.rb'))
      assert status.success?, out
      handle = env.client.workflow_handle('my-workflow-id')
      handle.result
      started = handle.fetch_history_events.first.workflow_execution_started_event_attributes
      assert_equal 'some-value',
                   Temporalio::SearchAttributes._from_proto(started.search_attributes)[MY_KEYWORD_KEY]
      eventually { assert_equal 'some-new-value', handle.describe.search_attributes[MY_KEYWORD_KEY] }
    end
  end
end
