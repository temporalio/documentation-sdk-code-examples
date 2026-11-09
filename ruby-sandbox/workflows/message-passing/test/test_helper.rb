# frozen_string_literal: true

require 'minitest/autorun'
require 'minitest/mock'
require 'securerandom'
require 'stringio'
require 'temporalio/testing'
require 'temporalio/worker'

# Several files in this folder define their own MyWorkflow, so each test file loads one of them and runs in its own
# process: `for f in workflows/message-passing/test/*_test.rb; do bundle exec ruby "$f"; done`.
class MessagePassingTest < Minitest::Test
  def self.env
    @@env ||= Temporalio::Testing::WorkflowEnvironment.start_local( # rubocop:disable Style/ClassVars
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ).tap { |env| Minitest.after_run { env.shutdown } }
  end

  def client
    self.class.env.client
  end

  def with_worker(task_queue: "message-passing-#{SecureRandom.uuid}", **, &)
    @task_queue = task_queue
    Temporalio::Worker.new(client:, task_queue:, **).run(&)
  end

  def start(workflow, *args)
    client.start_workflow(workflow, *args, id: "wf-#{SecureRandom.uuid}", task_queue: @task_queue)
  end

  # The page's client code connects to localhost:7233; point it at the test server instead.
  def with_stubbed_connect(&)
    Temporalio::Client.stub(:connect, client, &)
  end
end
