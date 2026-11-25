# frozen_string_literal: true

require 'minitest/autorun'
require 'tmpdir'
require 'temporalio/testing/workflow_environment'
require_relative 'run'

class ExternalStorageTest < Minitest::Test
  def verify_round_trip(driver)
    Temporalio::Testing::WorkflowEnvironment.start_local(
      data_converter: StorageConfiguration.data_converter(driver),
      dev_server_existing_path: ENV['TEMPORAL_CLI_PATH']
    ) do |env|
      result = ExternalStorageExample.run(env.client)
      assert_equal 3 * 1024 * 1024, result.fetch(:result_bytes)
      assert_equal 4, result.fetch(:reference_count)
      puts JSON.generate(result)
    end
  end

  def test_local_disk
    Dir.mktmpdir('ruby-external-storage-') do |directory|
      verify_round_trip(LocalDiskDriver.new(directory))
    end
  end

  def test_s3
    skip 'Set S3_BUCKET and configure AWS credentials for the live S3 test' unless ENV['S3_BUCKET']

    verify_round_trip(StorageConfiguration.s3_driver(ENV.fetch('S3_BUCKET')))
  end
end
