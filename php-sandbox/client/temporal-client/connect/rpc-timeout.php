<?php // @@@SNIPSTART php-client-rpc-timeout ?>
<?php
use App\Client\TemporalClient\MoneyTransfer\AccountTransferWorkflowInterface;
use Temporal\Client\GRPC\ServiceClient;
use Temporal\Client\WorkflowClient;

$serviceClient = ServiceClient::create('localhost:7233');
$workflowClient = WorkflowClient::create($serviceClient)
    ->withTimeout(5.75);

// Create a Workflow stub
$stub = $workflowClient->newWorkflowStub(AccountTransferWorkflowInterface::class);

// If the Workflow does not complete within 5.75 seconds, a TimeoutException will be thrown
$result = $stub->transfer('fromID', 'toID', 'refID', 1000);
// @@@SNIPEND
