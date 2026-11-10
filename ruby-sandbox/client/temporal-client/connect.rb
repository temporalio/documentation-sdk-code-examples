# frozen_string_literal: true

require 'temporalio/client'

def connect_local
  # @@@SNIPSTART ruby-client-connect-local
  client = Temporalio::Client.connect('localhost:7233', 'default')
  # @@@SNIPEND
  client
end

def connect_cloud_api_key
  # @@@SNIPSTART ruby-client-connect-cloud-api-key
  client = Temporalio::Client.connect(
    '<endpoint>', # Endpoint
    '<namespace_id>.<account_id>', # Namespace
    api_key: '<api_key>',
    tls: true
  )
  # @@@SNIPEND
  client
end

def connect_cloud_mtls
  # @@@SNIPSTART ruby-client-connect-cloud-mtls
  client = Temporalio::Client.connect(
    '<endpoint>', # Endpoint
    '<namespace_id>.<account_id>', # Namespace
    tls: Temporalio::Client::Connection::TLSOptions.new(
      client_cert: File.read('my-client-cert.pem'),
      client_private_key: File.read('my-client-key.pem')
    )
  )
  # @@@SNIPEND
  client
end

def rotate_client_certificate(my_worker)
  # @@@SNIPSTART ruby-client-rotate-certificate
  new_client = Temporalio::Client.connect(
    '<endpoint>', # Endpoint
    '<namespace_id>.<account_id>', # Namespace
    tls: Temporalio::Client::Connection::TLSOptions.new(
      client_cert: File.read('my-client-cert-new.pem'),
      client_private_key: File.read('my-client-key-new.pem')
    )
  )

  # my_worker is the Worker instance already running against the old client
  my_worker.client = new_client
  # @@@SNIPEND
end
