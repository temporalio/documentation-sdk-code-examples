# frozen_string_literal: true

require 'digest'
require 'fileutils'
require 'tempfile'
require 'temporalio/api'
require 'temporalio/converters/storage_driver'
require 'temporalio/converters/storage_driver_claim'

# @@@SNIPSTART ruby-external-storage-local-driver
# For development only. All Clients and Workers must share this directory.
class LocalDiskDriver < Temporalio::Converters::StorageDriver
  def initialize(directory)
    @directory = File.expand_path(directory)
    FileUtils.mkdir_p(@directory)
  end

  def name
    'local-disk'
  end

  def type
    'example.local-disk'
  end

  def store(_context, payloads)
    payloads.map do |payload|
      bytes = payload.to_proto
      key = Digest::SHA256.hexdigest(bytes)
      path = File.join(@directory, key)
      unless File.exist?(path)
        Tempfile.create('.payload-', @directory) do |file|
          file.binmode
          file.write(bytes)
          file.close
          begin
            # Publish the complete file without replacing another writer's file.
            File.link(file.path, path)
          rescue Errno::EEXIST
            # Another writer stored the same payload first.
          end
        end
      end
      Temporalio::Converters::StorageDriverClaim.new(claim_data: { 'key' => key })
    end
  end

  def retrieve(_context, claims)
    claims.map do |claim|
      key = claim.claim_data.fetch('key')
      raise 'Invalid payload key' unless /\A[0-9a-f]{64}\z/.match?(key)

      bytes = File.binread(File.join(@directory, key))
      raise 'Payload checksum mismatch' unless Digest::SHA256.hexdigest(bytes) == key

      Temporalio::Api::Common::V1::Payload.decode(bytes)
    end
  end
end
# @@@SNIPEND
