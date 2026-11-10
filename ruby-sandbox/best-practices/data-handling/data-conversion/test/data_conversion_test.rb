# frozen_string_literal: true

require 'minitest/autorun'
require 'securerandom'
require 'temporalio/client'
require 'temporalio/testing'
require 'temporalio/worker'
require_relative '../greeting_workflow'

class DataConversionTest < Minitest::Test
  def test_payload_round_trip
    converter = Temporalio::Converters::DataConverter.default.payload_converter
    payload = converter.to_payload(GreetingInput.new(name: 'Temporal', count: 1))
    assert_equal 'json/plain', payload.metadata['encoding']
    assert_equal({ 'name' => 'Temporal', 'count' => 1, 'json_class' => 'GreetingInput' }, JSON.parse(payload.data))

    value = converter.from_payload(payload)
    assert_instance_of GreetingInput, value
    assert_equal 'Temporal', value.name
    assert_equal 1, value.count
  end

  def test_workflow_and_activity_receive_model
    Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ) do |env|
      worker = Temporalio::Worker.new(client: env.client, task_queue: "tq-#{SecureRandom.uuid}",
                                      workflows: [GreetingWorkflow], activities: [ComposeGreeting])
      result = worker.run do
        env.client.execute_workflow(GreetingWorkflow, GreetingInput.new(name: 'Temporal', count: 1),
                                    id: "wf-#{SecureRandom.uuid}", task_queue: worker.task_queue)
      end
      assert_instance_of GreetingInput, result
      assert_equal 'Hello, Temporal!', result.name
      assert_equal 2, result.count
    end
  end
end
