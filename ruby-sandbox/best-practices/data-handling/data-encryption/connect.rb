# frozen_string_literal: true

require 'temporalio/client'
require 'temporalio/converters/data_converter'
require_relative 'base64_codec'

# @@@SNIPSTART ruby-data-encryption-client
my_client = Temporalio::Client.connect(
  'localhost:7233',
  'my-namespace',
  data_converter: Temporalio::Converters::DataConverter.new(payload_codec: Base64Codec.new)
)
# @@@SNIPEND

puts "Connected to #{my_client.namespace}"
