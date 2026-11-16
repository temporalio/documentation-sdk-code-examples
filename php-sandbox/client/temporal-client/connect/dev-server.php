<?php // @@@SNIPSTART php-client-connect-dev ?>
<?php
use Temporal\Client\GRPC\ServiceClient;
use Temporal\Client\WorkflowClient;

$serviceClient = ServiceClient::create('localhost:7233');
$workflowClient = WorkflowClient::create($serviceClient);

// Use $workflowClient to work with Workflows ...
// @@@SNIPEND
