<?php
declare(strict_types=1);

// Runs the Client code from the Enriching the UI page against a fake Temporal
// Service, without the grpc extension. start-workflow.php gets a ServiceClient
// whose gRPC stub is a fake, and an interceptor records each request and
// returns a canned response. Run from php-sandbox/:
//   php platform/enriching-ui/test-client.php

use App\Platform\EnrichingUi\AsyncStart;
use App\Platform\EnrichingUi\TimerSummary\YourWorkflow;
use Temporal\Api\Enums\V1\EventType;
use Temporal\Api\History\V1\History;
use Temporal\Api\History\V1\HistoryEvent;
use Temporal\Api\History\V1\WorkflowExecutionCompletedEventAttributes;
use Temporal\Api\Workflowservice\V1 as WS;
use Temporal\Client\GRPC\ContextInterface;
use Temporal\Client\GRPC\ServiceClient;
use Temporal\Client\WorkflowClient;
use Temporal\DataConverter\DataConverter;
use Temporal\DataConverter\EncodedValues;
use Temporal\Interceptor\GrpcClientInterceptor;
use Temporal\Internal\Interceptor\Pipeline;

require __DIR__ . '/../../vendor/autoload.php';

$fake = new class implements GrpcClientInterceptor {
    /** @var list<array{string, object}> */
    public array $requests = [];

    public function interceptCall(string $method, object $arg, ContextInterface $ctx, callable $next): object
    {
        $this->requests[] = [$method, $arg];
        return match ($method) {
            'StartWorkflowExecution' => (new WS\StartWorkflowExecutionResponse())->setRunId('run-id')->setStarted(true),
            'GetWorkflowExecutionHistory' => (new WS\GetWorkflowExecutionHistoryResponse())->setHistory(
                (new History())->setEvents([
                    (new HistoryEvent())
                        ->setEventId(1)
                        ->setEventType(EventType::EVENT_TYPE_WORKFLOW_EXECUTION_COMPLETED)
                        ->setWorkflowExecutionCompletedEventAttributes(
                            (new WorkflowExecutionCompletedEventAttributes())->setResult(
                                EncodedValues::fromValues(['Timer completed'], DataConverter::createDefault())->toPayloads(),
                            ),
                        ),
                ]),
            ),
            default => new ("Temporal\\Api\\Workflowservice\\V1\\{$method}Response")(),
        };
    }

    /** @return list<object> */
    public function of(string $method): array
    {
        return \array_values(\array_map(fn($r) => $r[1], \array_filter($this->requests, fn($r) => $r[0] === $method)));
    }
};
$serviceClient = (new ServiceClient(static fn() => new class extends \Grpc\BaseStub {
    public function __construct() {}

    public function close(): void {}
}))->withInterceptorPipeline(Pipeline::prepare([$fake]));

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
$decode = static fn($payload) => DataConverter::createDefault()->fromPayload($payload, null);

// Start a Workflow with a static summary and details, and wait for its result.
include __DIR__ . '/start-workflow.php';
$request = $fake->of('StartWorkflowExecution')[0];
check('workflow id', 'your-workflow-id', $request->getWorkflowId());
check('task queue', 'your-task-queue', $request->getTaskQueue()->getName());
check('static summary', 'Order processing for customer #12345', $decode($request->getUserMetadata()->getSummary()));
check('static details', 'Processing premium order with expedited shipping', $decode($request->getUserMetadata()->getDetails()));
check('result', 'Timer completed', $result);

// Start asynchronously: no result request.
$before = \count($fake->of('GetWorkflowExecutionHistory'));
$client = WorkflowClient::create($serviceClient);
(new AsyncStart())->start($client, $client->newWorkflowStub(YourWorkflow::class));
check('async start', 2, \count($fake->of('StartWorkflowExecution')));
check('async no wait', $before, \count($fake->of('GetWorkflowExecutionHistory')));

exit($failures > 0 ? 1 : 0);
