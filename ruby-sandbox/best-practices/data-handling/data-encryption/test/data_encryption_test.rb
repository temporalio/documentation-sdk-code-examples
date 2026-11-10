# frozen_string_literal: true

require 'minitest/autorun'
require 'minitest/mock'
require 'securerandom'
require 'temporalio/client'
require 'temporalio/testing'
require 'temporalio/worker'
require 'temporalio/workflow'

class EchoWorkflow < Temporalio::Workflow::Definition
  def execute(input)
    "Hello, #{input}!"
  end
end

class DataEncryptionTest < Minitest::Test
  def test_codec_round_trip
    require_relative '../base64_codec'
    converter = Temporalio::Converters::PayloadConverter.default
    original = converter.to_payload('secret')
    encoded = Base64Codec.new.encode([original]).first
    assert_equal 'binary/my-payload-encoding', encoded.metadata['encoding']
    refute_includes encoded.data, 'secret'
    assert_equal original, Base64Codec.new.decode([encoded]).first
    # Payloads without the codec's encoding pass through
    assert_same original, Base64Codec.new.decode([original]).first
  end

  def test_client_encodes_payloads
    Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ) do |env|
      connect_args = nil
      my_client = nil
      original_connect = Temporalio::Client.method(:connect)
      fake_connect = lambda do |target, namespace, **kwargs|
        connect_args = [target, namespace]
        my_client = original_connect.call(env.client.connection.target_host, 'default', **kwargs)
      end
      Temporalio::Client.stub(:connect, fake_connect) do
        assert_output(/Connected to default/) { load File.expand_path('../connect.rb', __dir__) }
      end
      assert_equal ['localhost:7233', 'my-namespace'], connect_args
      assert_instance_of Base64Codec, my_client.data_converter.payload_codec

      # A Worker that isn't fiber based needs a thread pool to run the codec in
      worker = Temporalio::Worker.new(client: my_client, task_queue: "tq-#{SecureRandom.uuid}",
                                      workflows: [EchoWorkflow],
                                      workflow_payload_codec_thread_pool: Temporalio::Worker::ThreadPool.default)
      handle = nil
      result = worker.run do
        handle = my_client.start_workflow(EchoWorkflow, 'secret', id: "wf-#{SecureRandom.uuid}",
                                                                  task_queue: worker.task_queue)
        handle.result
      end
      assert_equal 'Hello, secret!', result

      # A Client without the codec sees only encoded payloads in history
      events = env.client.workflow_handle(handle.id).fetch_history_events.to_a
      input = events.first.workflow_execution_started_event_attributes.input.payloads.first
      output = events.last.workflow_execution_completed_event_attributes.result.payloads.first
      [input, output].each do |payload|
        assert_equal 'binary/my-payload-encoding', payload.metadata['encoding']
        refute_includes payload.data, 'secret'
      end
    end
  end
end
