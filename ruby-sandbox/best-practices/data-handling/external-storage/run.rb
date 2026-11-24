# frozen_string_literal: true

require 'json'
require 'securerandom'
require 'temporalio/env_config'
require_relative 'configuration'
require_relative 'local_disk_driver'
require_relative 'large_payload_workflow'

module ExternalStorageExample
  def self.run(client)
    task_queue = "external-storage-#{SecureRandom.uuid}"
    # @@@SNIPSTART ruby-external-storage-worker
    worker = Temporalio::Worker.new(
      client: client,
      task_queue: task_queue,
      workflows: [LargePayloadWorkflow],
      activities: [LargePayloadActivity],
      workflow_payload_codec_thread_pool: Temporalio::Worker::ThreadPool.new
    )
    # @@@SNIPEND
    worker.run do
      input = 'x' * (3 * 1024 * 1024)
      handle = client.start_workflow(LargePayloadWorkflow, input,
                                     id: "external-storage-#{SecureRandom.uuid}", task_queue: task_queue,
                                     execution_timeout: 60)
      raise 'Returned payload differs from input' unless handle.result == input

      payloads = handle.fetch_history.events.flat_map do |event|
        attributes = event.workflow_execution_started_event_attributes || event.activity_task_scheduled_event_attributes ||
                     event.activity_task_completed_event_attributes || event.workflow_execution_completed_event_attributes
        next [] unless attributes

        values = attributes.respond_to?(:input) ? attributes.input : attributes.result
        values ? values.payloads.to_a : []
      end
      unless payloads.size == 4 && payloads.all? { |p| p.metadata['messageType'] == 'temporal.api.sdk.v1.ExternalStorageReference' && p.to_proto.bytesize < 1024 }
        raise 'Expected four small External Storage references in Event History'
      end
      { result_bytes: input.bytesize, reference_count: payloads.size,
        largest_reference_bytes: payloads.map { |p| p.to_proto.bytesize }.max }
    end
  end
end

if $PROGRAM_NAME == __FILE__
  driver = if ENV['S3_BUCKET']
             StorageConfiguration.s3_driver(ENV.fetch('S3_BUCKET'))
           else
             LocalDiskDriver.new(ENV.fetch('PAYLOAD_STORE_DIR', 'payload-store'))
           end
  data_converter = StorageConfiguration.data_converter(driver)
  # @@@SNIPSTART ruby-external-storage-client
  args, options = Temporalio::EnvConfig::ClientConfig.load_client_connect_options
  args[0] ||= 'localhost:7233'
  args[1] ||= 'default'
  client = Temporalio::Client.connect(*args, **options, data_converter: data_converter)
  # @@@SNIPEND
  puts JSON.generate(ExternalStorageExample.run(client))
end
