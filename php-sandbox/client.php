<?php // @@@SNIPSTART php-quickstart-client ?>
<?php
declare(strict_types=1);

use Temporal\Client\GRPC\ServiceClient;
use Temporal\Client\WorkflowClient;

ini_set('display_errors', 'stderr');
require "vendor/autoload.php";

$client = new WorkflowClient(
    ServiceClient::create('localhost:7233'),
);

$workflowStub = $client->newWorkflowStub(\App\SayHelloWorkflow::class);
$result = $workflowStub->sayHello('Temporal');

echo "Result: {$result}\n";
// @@@SNIPEND
