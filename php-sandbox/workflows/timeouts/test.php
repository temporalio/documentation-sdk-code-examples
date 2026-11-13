<?php
declare(strict_types=1);

// Checks the Workflow options from the Workflow Timeouts page without a
// Temporal Service or the grpc extension. A fake gRPC stub stands in for the
// connection, and an interceptor records each request the SDK sends and
// returns an empty response. Run from php-sandbox/:
//   php workflows/timeouts/test.php

use App\Workflows\Timeouts\WorkflowStarter;
use Temporal\Api\Enums\V1\WorkflowIdReusePolicy;
use Temporal\Client\GRPC\ContextInterface;
use Temporal\Client\GRPC\ServiceClient;
use Temporal\Client\WorkflowClient;
use Temporal\Interceptor\GrpcClientInterceptor;
use Temporal\Internal\Interceptor\Pipeline;

require __DIR__ . '/../../vendor/autoload.php';

final class RecordingInterceptor implements GrpcClientInterceptor
{
    /** @var array<string, object> Last request per RPC method */
    public array $requests = [];

    public function interceptCall(string $method, object $arg, ContextInterface $ctx, callable $next): object
    {
        $this->requests[$method] = $arg;
        $response = "Temporal\\Api\\Workflowservice\\V1\\{$method}Response";
        return new $response();
    }
}

$recorder = new RecordingInterceptor();
$serviceClient = (new ServiceClient(static fn() => new class extends \Grpc\BaseStub {
    public function __construct() {}

    public function close(): void {}
}))->withInterceptorPipeline(Pipeline::prepare([$recorder]));
$client = new WorkflowClient($serviceClient);
$starter = new WorkflowStarter($client);

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

$client->start($starter->newWorkflowWithTimeout());
$request = $recorder->requests['StartWorkflowExecution'];
check('timeout workflow type', 'MyWorkflowInterface', $request->getWorkflowType()->getName());
check('timeout workflow id', 'my-workflow-id', $request->getWorkflowId());
check('id reuse policy', WorkflowIdReusePolicy::WORKFLOW_ID_REUSE_POLICY_ALLOW_DUPLICATE, $request->getWorkflowIdReusePolicy());
check('execution timeout', 120, (int) $request->getWorkflowExecutionTimeout()?->getSeconds());
check('run timeout unset', 0, (int) $request->getWorkflowRunTimeout()?->getSeconds());

$client->start($starter->newWorkflowWithRetryPolicy());
$request = $recorder->requests['StartWorkflowExecution'];
check('retry initial interval', 120, (int) $request->getRetryPolicy()?->getInitialInterval()?->getSeconds());

exit($failures > 0 ? 1 : 0);
