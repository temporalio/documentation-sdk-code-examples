<?php // @@@SNIPSTART php-client-rpc-retry ?>
<?php
use Temporal\Client\Common\RpcRetryOptions;
use Temporal\Client\GRPC\ServiceClient;
use Temporal\Client\WorkflowClient;

$serviceClient = ServiceClient::create('localhost:7233');
$workflowClient = WorkflowClient::create($serviceClient)
    ->withRetryOptions(
        RpcRetryOptions::new()
            ->withMaximumAttempts(10)
            ->withInitialInterval('1 second')   // The first retry will be in 1 second
            ->withBackoffCoefficient(2.5)       // Each next retry time will be multiplied by 2.5
            ->withMaximumInterval('20 seconds') // The maximum interval between attempts
            ->withMaximumJitterCoefficient(0.2) // Actual retry time can be +/- 20% of the calculated time
    );
// @@@SNIPEND
