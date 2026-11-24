# frozen_string_literal: true

require 'temporalio/client'
require 'temporalio/worker'
require 'temporalio/converters/external_storage'
require 'temporalio/contrib/aws/s3_driver'
require 'temporalio/contrib/aws/s3_driver/aws_sdk_s3_storage_driver_client'

module StorageConfiguration
  def self.s3_driver(bucket)
    # @@@SNIPSTART ruby-external-storage-s3-create
    s3_client = Aws::S3::Client.new
    driver = Temporalio::Contrib::Aws::S3Driver::S3StorageDriver.new(
      client: Temporalio::Contrib::Aws::S3Driver::AwsSdkS3StorageDriverClient.new(s3_client),
      bucket: bucket
    )
    # @@@SNIPEND
    driver
  end

  def self.data_converter(driver)
    # @@@SNIPSTART ruby-external-storage-converter
    data_converter = Temporalio::Converters::DataConverter.new(
      external_storage: Temporalio::Converters::ExternalStorage.new(drivers: [driver])
    )
    # @@@SNIPEND
    data_converter
  end

  def self.threshold(driver)
    # @@@SNIPSTART ruby-external-storage-threshold
    storage = Temporalio::Converters::ExternalStorage.new(
      drivers: [driver],
      payload_size_threshold: 256 * 1024
    )
    # @@@SNIPEND
    storage
  end

  def self.migration(preferred, legacy)
    # @@@SNIPSTART ruby-external-storage-multiple-drivers
    storage = Temporalio::Converters::ExternalStorage.new(
      drivers: [preferred, legacy],
      driver_selector: ->(_context, _payload) { preferred }
    )
    # @@@SNIPEND
    storage
  end
end
