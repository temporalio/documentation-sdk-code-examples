<?php // @@@SNIPSTART php-client-connect-cloud ?>
<?php
use Temporal\Client\ClientOptions;
use Temporal\Client\GRPC\ServiceClient;
use Temporal\Client\WorkflowClient;

$serviceClient = \Temporal\Client\GRPC\ServiceClient::createSSL(
    address: '<your-custom-namespace>.tmprl.cloud:7233',
    // crt: 'certs/server-root-ca-cert.pem', # ROOT CA to validate the server cert
    clientKey: 'certs/client-private-key.pem',
    clientPem: 'certs/client-cert.pem',
    // overrideServerName: 'tls-sample',
);

$workflowClient = WorkflowClient::create(
    serviceClient: $serviceClient,
    options: (new ClientOptions())
        ->withNamespace('<your-custom-namespace>.<id>'),
);
// @@@SNIPEND
