<?php
declare(strict_types=1);

// Runs the Client code from the Observability page against a fake Temporal
// Service, without the grpc extension. A fake gRPC stub stands in for the
// connection, and an interceptor records each request the SDK sends and
// returns a canned response. Run from php-sandbox/:
//   php platform/observability/test-client.php

use App\Platform\Observability\ClientExamples;
use Temporal\Api\Common\V1\WorkflowExecution;
use Temporal\Api\Common\V1\WorkflowType;
use Temporal\Api\Enums\V1\WorkflowExecutionStatus;
use Temporal\Api\Workflow\V1\WorkflowExecutionInfo;
use Temporal\Api\Workflowservice\V1 as WS;
use Temporal\Client\GRPC\ContextInterface;
use Temporal\Client\GRPC\ServiceClient;
use Temporal\Client\WorkflowClient;
use Temporal\Interceptor\GrpcClientInterceptor;
use Temporal\Internal\Interceptor\Pipeline;

require __DIR__ . '/../../vendor/autoload.php';

$fake = new class implements GrpcClientInterceptor {
    /** @var array<string, object> Last request per RPC method */
    public array $requests = [];

    public function interceptCall(string $method, object $arg, ContextInterface $ctx, callable $next): object
    {
        $this->requests[$method] = $arg;
        return match ($method) {
            'ListWorkflowExecutions' => (new WS\ListWorkflowExecutionsResponse())->setExecutions(\array_map(
                static fn(string $id) => (new WorkflowExecutionInfo())
                    ->setExecution((new WorkflowExecution())->setWorkflowId($id)->setRunId("{$id}-run"))
                    ->setType((new WorkflowType())->setName('GreetingWorkflow'))
                    ->setStatus(WorkflowExecutionStatus::WORKFLOW_EXECUTION_STATUS_COMPLETED),
                ['greeting-1', 'greeting-2'],
            )),
            'StartWorkflowExecution' => (new WS\StartWorkflowExecutionResponse())->setRunId('run-id')->setStarted(true),
            default => new ("Temporal\\Api\\Workflowservice\\V1\\{$method}Response")(),
        };
    }
};
$serviceClient = (new ServiceClient(static fn() => new class extends \Grpc\BaseStub {
    public function __construct() {}

    public function close(): void {}
}))->withInterceptorPipeline(Pipeline::prepare([$fake]));
$client = new WorkflowClient($serviceClient);
$examples = new ClientExamples();

$failures = 0;
function check(string $name, mixed $expected, mixed $actual): void
{
    global $failures;
    if ($expected === $actual) {
        echo "ok   {$name}\n";
    } else {
        echo "FAIL {$name}: expected " . var_export($expected, true) . ', got ' . var_export($actual, true) . "\n";
        $failures++;
    }
}

// List Workflow Executions
ob_start();
$examples->listWorkflows($client);
$output = ob_get_clean();
check('list query', 'WorkflowType="GreetingWorkflow"', $fake->requests['ListWorkflowExecutions']->getQuery());
check('list output', "Workflow ID: greeting-1\nWorkflow ID: greeting-2\n", $output);

// Set Search Attributes when starting a Workflow
$client->start($examples->startWithSearchAttributes($client, 'order-42'));
$request = $fake->requests['StartWorkflowExecution'];
check('start type', 'OrderWorkflowInterface', $request->getWorkflowType()->getName());
check('start execution timeout', 600, (int) $request->getWorkflowExecutionTimeout()?->getSeconds());
$attributes = [];
foreach ($request->getSearchAttributes()->getIndexedFields() as $name => $payload) {
    $attributes[$name] = json_decode($payload->getData(), true);
}
ksort($attributes);
// The Temporal Service takes each attribute's type from its registration;
// test.sh checks them against a dev server with registered attributes.
check('search attributes', [
    'DestinationTime' => '2028-11-05T00:10:07+00:00',
    'OrderId' => 'order-42',
], $attributes);

exit($failures > 0 ? 1 : 0);
