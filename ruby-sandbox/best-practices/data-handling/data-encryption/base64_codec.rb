# frozen_string_literal: true

require 'base64'
require 'temporalio/api'
require 'temporalio/converters/payload_codec'

# @@@SNIPSTART ruby-data-encryption-codec
class Base64Codec < Temporalio::Converters::PayloadCodec
  def encode(payloads)
    payloads.map do |p|
      Temporalio::Api::Common::V1::Payload.new(
        # Set our specific encoding. We may also want to add a key ID in here for use by
        # the decode side
        metadata: { 'encoding' => 'binary/my-payload-encoding' },
        data: Base64.strict_encode64(p.to_proto)
      )
    end
  end

  def decode(payloads)
    payloads.map do |p|
      # Ignore if it doesn't have our expected encoding
      next p unless p.metadata['encoding'] == 'binary/my-payload-encoding'

      Temporalio::Api::Common::V1::Payload.decode(
        Base64.strict_decode64(p.data)
      )
    end
  end
end
# @@@SNIPEND
