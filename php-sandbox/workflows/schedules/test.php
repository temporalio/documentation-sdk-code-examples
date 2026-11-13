<?php
declare(strict_types=1);

// Checks the Workflow options from the Schedules page without a Temporal
// Service or the grpc extension. A fake gRPC stub stands in for the
// connection, and an interceptor records each request the SDK sends. The
// second start of the Cron Workflow gets the "already started" error a real
// Temporal Service returns. Run from php-sandbox/:
//   php workflows/schedules/test.php

use App\Workflows\Schedules\WorkflowStarter;
use Google\Protobuf\Any;
use Google\Rpc\Status;
use Temporal\Api\Errordetails\V1\WorkflowExecutionAlreadyStartedFailure;
use Temporal\Api\Workflowservice\V1\StartWorkflowExecutionResponse;
use Temporal\Client\GRPC\ContextInterface;
use Temporal\Client\GRPC\ServiceClient;
use Temporal\Client\GRPC\StatusCode;
use Temporal\Client\WorkflowClient;
use Temporal\DataConverter\DataConverter;
use Temporal\DataConverter\EncodedValues;
use Temporal\Exception\Client\ServiceClientException;
use Temporal\Interceptor\GrpcClientInterceptor;
use Temporal\Internal\Interceptor\Pipeline;

require __DIR__ . '/../../vendor/autoload.php';

final class RecordingInterceptor implements GrpcClientInterceptor
{
    /** @var list<object> StartWorkflowExecution requests */
    public array $starts = [];
    /** @var array<string, true> Workflow IDs that are running */
    private array $running = [];

    public function interceptCall(string $method, object $arg, ContextInterface $ctx, callable $next): object
    {
        if ($method !== 'StartWorkflowExecution') {
            return new ("Temporal\\Api\\Workflowservice\\V1\\{$method}Response")();
        }

        $this->starts[] = $arg;
        $id = $arg->getWorkflowId();
        if (isset($this->running[$id])) {
            $details = new Any();
            $details->pack((new WorkflowExecutionAlreadyStartedFailure())->setRunId('first-run'));
            $status = (new Status())->setCode(StatusCode::ALREADY_EXISTS)->setDetails([$details]);
            throw new ServiceClientException((object) [
                'code' => StatusCode::ALREADY_EXISTS,
                'details' => 'Workflow execution is already running',
                'metadata' => ['grpc-status-details-bin' => [$status->serializeToString()]],
            ]);
        }
        $this->running[$id] = true;

        return (new StartWorkflowExecutionResponse())->setRunId('first-run')->setStarted(true);
    }
}

$recorder = new RecordingInterceptor();
$serviceClient = (new ServiceClient(static fn() => new class extends \Grpc\BaseStub {
    public function __construct() {}

    public function close(): void {}
}))->withInterceptorPipeline(Pipeline::prepare([$recorder]));
$client = new WorkflowClient($serviceClient);
$starter = new WorkflowStarter();

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
$args = static fn($request): array => EncodedValues::fromPayloads($request->getInput(), DataConverter::createDefault())->getValues();

// Start Delay
$starter->startWithDelay($client);
$request = end($recorder->starts);
check('start-delay type', 'GreeterWorkflowInterface', $request->getWorkflowType()->getName());
check('start-delay seconds', 600, (int) $request->getWorkflowStartDelay()?->getSeconds());
check('start-delay args', ['Hello world!'], $args($request));

// Cron Job, started twice
ob_start();
$starter->startCronWorkflow($client);
$starter->startCronWorkflow($client);
$output = ob_get_clean();
$request = end($recorder->starts);
check('cron type', 'CronWorkflowInterface', $request->getWorkflowType()->getName());
check('cron workflow id', 'cron-workflow', $request->getWorkflowId());
check('cron schedule', '* * * * *', $request->getCronSchedule());
check('cron execution timeout', 600, (int) $request->getWorkflowExecutionTimeout()?->getSeconds());
check('cron run timeout', 60, (int) $request->getWorkflowRunTimeout()?->getSeconds());
check('cron args', ['Antony'], $args($request));
check(
    'cron output',
    "Starting CronWorkflow...\nStarted: WorkflowID=cron-workflow\n"
        . "Starting CronWorkflow...\nStill running as WorkflowID=cron-workflow\n",
    $output,
);

exit($failures > 0 ? 1 : 0);
