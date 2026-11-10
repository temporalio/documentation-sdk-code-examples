# frozen_string_literal: true

require 'minitest/autorun'
require 'open3'
require 'rbconfig'
require 'securerandom'
require 'temporalio/client'
require 'temporalio/testing'
require 'temporalio/worker'
require_relative '../execute_workflow'
require_relative '../your_activity'

# Each Workflow file defines YourWorkflow, so each loads into its own module.
# The Workflow Type is still YourWorkflow.
module Examples
  def self.load(module_name, file)
    path = File.expand_path("../#{file}", __dir__)
    mod = const_set(module_name, Module.new)
    mod.module_eval(File.read(path), path)
    mod.const_get(:YourWorkflow)
  end
  CURRENT_DETAILS = load(:CurrentDetails, 'current_details_workflow.rb')
  ACTIVITY_SUMMARY = load(:ActivitySummary, 'activity_summary_workflow.rb')
  TIMER_SUMMARY = load(:TimerSummary, 'timer_summary_workflow.rb')
end

# start_workflow.rb connects to localhost:7233, so the test runs a dev server
# on that port. It listens on IPv4 and IPv6 because localhost can resolve to either.
class EnrichingUiTest < Minitest::Test
  DIR = File.expand_path('..', __dir__)

  def self.client
    @client ||= begin
      pid = spawn(ENV.fetch('TEMPORAL_CLI_PATH', 'temporal'), 'server', 'start-dev', '--headless', '--ip', '::',
                  '--port', '7233', out: File::NULL, err: File::NULL)
      Minitest.after_run do
        Process.kill('TERM', pid)
        Process.wait(pid)
      end
      deadline = Time.now + 30
      begin
        Temporalio::Client.connect('127.0.0.1:7233', 'default')
      rescue Temporalio::Internal::Bridge::Error
        raise if Time.now > deadline

        sleep 0.3
        retry
      end
    end
  end

  def client = self.class.client

  def decode(payload) = client.data_converter.payload_converter.from_payload(payload)

  def with_worker(workflow, activities: [], &)
    Temporalio::Worker.new(client:, task_queue: 'your-task-queue', workflows: [workflow], activities:).run(&)
  end

  def test_start_and_execute_with_static_summary_and_details
    with_worker(Examples::CURRENT_DETAILS) do
      out, status = Open3.capture2e(RbConfig.ruby, File.join(DIR, 'start_workflow.rb'))
      assert status.success?, out
      handle = client.workflow_handle('your-workflow-id')
      assert_equal 'Workflow completed', handle.result
      desc = handle.describe
      assert_equal 'Order processing for customer #12345', desc.static_summary
      assert_equal 'Processing premium order with expedited shipping', desc.static_details

      # execute_workflow takes the same options and waits for the result.
      assert_equal 'Workflow completed', execute_your_workflow(client)
      assert_equal 'Order processing for customer #12345', handle.describe.static_summary

      # The Workflow set its current details.
      metadata = handle.query('__temporal_workflow_metadata')
      assert_equal 'Updated workflow details with new status', metadata.current_details
    end
  end

  def test_activity_summary
    with_worker(Examples::ACTIVITY_SUMMARY, activities: [YourActivity]) do
      handle = client.start_workflow('YourWorkflow', 'input', id: "wf-#{SecureRandom.uuid}", task_queue: 'your-task-queue')
      assert_equal 'processed input', handle.result
      scheduled = handle.fetch_history_events.find(&:activity_task_scheduled_event_attributes)
      assert_equal 'Processing user data', decode(scheduled.user_metadata.summary)
    end
  end

  def test_timer_summary
    Temporalio::Testing::WorkflowEnvironment.start_time_skipping do |env|
      Temporalio::Worker.new(client: env.client, task_queue: 'your-task-queue', workflows: [Examples::TIMER_SUMMARY]).run do
        handle = env.client.start_workflow('YourWorkflow', 'input', id: "wf-#{SecureRandom.uuid}",
                                                                    task_queue: 'your-task-queue')
        assert_equal 'Timer completed', handle.result
        timer = handle.fetch_history_events.find(&:timer_started_event_attributes)
        assert_equal 300, timer.timer_started_event_attributes.start_to_fire_timeout.seconds
        assert_equal 'Waiting for payment confirmation',
                     env.client.data_converter.payload_converter.from_payload(timer.user_metadata.summary)
      end
    end
  end
end
