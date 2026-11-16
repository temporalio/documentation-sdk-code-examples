<?php
declare(strict_types=1);

// Runs the Client code from the Temporal Client page against a fake Temporal
// Service, without the grpc extension. A fake gRPC stub stands in for the
// connection, and an interceptor records each request the SDK sends and
// returns a canned response. The connection scripts in connect/ can't run
// without grpc; phpstan.neon checks them. Run from php-sandbox/:
//   php client/temporal-client/test-client.php

use App\Client\TemporalClient\ClientExamples;
use App\Client\TemporalClient\MoneyTransfer\AccountTransferWorkflowInterface as MoneyTransferWorkflowInterface;
use Ramsey\Uuid\Uuid;
use Ramsey\Uuid\UuidInterface;
use Temporal\Api\Common\V1\WorkflowExecution;
use Temporal\Api\Common\V1\WorkflowType;
use Temporal\Api\Enums\V1\EventType;
use Temporal\Api\Enums\V1\UpdateWorkflowExecutionLifecycleStage as Stage;
use Temporal\Api\Enums\V1\WorkflowExecutionStatus;
use Temporal\Api\History\V1\History;
use Temporal\Api\History\V1\HistoryEvent;
use Temporal\Api\History\V1\WorkflowExecutionCompletedEventAttributes;
use Temporal\Api\Update\V1\Outcome;
use Temporal\Api\Update\V1\UpdateRef;
use Temporal\Api\Workflow\V1\WorkflowExecutionConfig;
use Temporal\Api\Workflow\V1\WorkflowExecutionInfo;
use Temporal\Api\Workflowservice\V1 as WS;
use Temporal\Client\GRPC\ContextInterface;
use Temporal\Client\GRPC\ServiceClient;
use Temporal\Client\WorkflowClient;
use Temporal\DataConverter\DataConverter;
use Temporal\DataConverter\EncodedValues;
use Temporal\Interceptor\GrpcClientInterceptor;
use Temporal\Internal\Interceptor\Pipeline;

require __DIR__ . '/../../vendor/autoload.php';

final class FakeService implements GrpcClientInterceptor
{
    /** @var list<array{string, object}> Every request, in order */
    public array $requests = [];
    /** @var list<mixed> Values to return as Workflow results */
    public array $results = [];

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
                                EncodedValues::fromValues([\array_shift($this->results)], DataConverter::createDefault())->toPayloads(),
                            ),
                        ),
                ]),
            ),
            'UpdateWorkflowExecution' => (new WS\UpdateWorkflowExecutionResponse())
                ->setUpdateRef((new UpdateRef())->setUpdateId($arg->getRequest()->getMeta()->getUpdateId())->setWorkflowExecution(
                    (new WorkflowExecution())->setWorkflowId($arg->getWorkflowExecution()->getWorkflowId())->setRunId('run-id'),
                ))
                ->setStage(Stage::UPDATE_WORKFLOW_EXECUTION_LIFECYCLE_STAGE_COMPLETED)
                ->setOutcome((new Outcome())->setSuccess(EncodedValues::fromValues([null], DataConverter::createDefault())->toPayloads())),
            'DescribeWorkflowExecution' => (new WS\DescribeWorkflowExecutionResponse())
                ->setExecutionConfig(new WorkflowExecutionConfig())
                ->setWorkflowExecutionInfo(
                (new WorkflowExecutionInfo())
                    ->setExecution($arg->getExecution())
                    ->setType((new WorkflowType())->setName('MoneyTransfer'))
                    ->setStatus(WorkflowExecutionStatus::WORKFLOW_EXECUTION_STATUS_RUNNING),
            ),
            default => new ("Temporal\\Api\\Workflowservice\\V1\\{$method}Response")(),
        };
    }

    /** @return list<object> Requests of one RPC method, in order */
    public function of(string $method): array
    {
        return \array_values(\array_map(fn($r) => $r[1], \array_filter($this->requests, fn($r) => $r[0] === $method)));
    }

    public function reset(array $results = []): void
    {
        $this->requests = [];
        $this->results = $results;
    }
}

$fake = new FakeService();
$serviceClient = (new ServiceClient(static fn() => new class extends \Grpc\BaseStub {
    public function __construct() {}

    public function close(): void {}

    // Report a ready channel, so connect() succeeds.
    public function getConnectivityState($try_to_connect = false): int
    {
        return \Temporal\Client\GRPC\Connection\ConnectionState::Ready->value;
    }

    public function waitForReady($timeout): bool
    {
        return true;
    }
}))->withInterceptorPipeline(Pipeline::prepare([$fake]));
$client = new WorkflowClient($serviceClient);
$examples = new ClientExamples();
$moneyTransfer = new \App\Client\TemporalClient\MoneyTransferExamples();
$args = static fn($payloads): array => $payloads === null ? [] : EncodedValues::fromPayloads($payloads, DataConverter::createDefault())->getValues();

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

$tx = Uuid::uuid4();
[$a, $b, $c, $d] = [Uuid::uuid4(), Uuid::uuid4(), Uuid::uuid4(), Uuid::uuid4()];

// Typed and untyped stubs send the same requests.
foreach (['typedStub', 'untypedStub'] as $method) {
    $fake->reset();
    $examples->$method($client, $tx, $a, $b, 100, $c, $d, 200);
    $start = $fake->of('StartWorkflowExecution')[0];
    check("{$method} type", 'account.transfer', $start->getWorkflowType()->getName());
    check("{$method} start args", [$tx->toString()], $args($start->getInput()));
    $updates = \array_map(fn($u) => [$u->getRequest()->getInput()->getName(), $args($u->getRequest()->getInput()->getArgs())], $fake->of('UpdateWorkflowExecution'));
    check("{$method} updates", [
        ['pay', [$a->toString(), $b->toString(), 100]],
        ['pay', [$c->toString(), $d->toString(), 200]],
        ['finish', []],
    ], $updates);
}

// Start a Workflow synchronously
$confirmation = '0195f0a4-4b5e-7d0e-8a5f-7a1b2c3d4e5f';
$fake->reset([$confirmation]);
$result = $moneyTransfer->startSync($client);
check('start-sync type', 'MoneyTransfer', $fake->of('StartWorkflowExecution')[0]->getWorkflowType()->getName());
check('start-sync args', ['fromID', 'toID', 'refID', 1000], $args($fake->of('StartWorkflowExecution')[0]->getInput()));
check('start-sync result', $confirmation, $result instanceof UuidInterface ? $result->toString() : $result);

// Start a Workflow asynchronously: no result request
$fake->reset();
$run = $moneyTransfer->startAsync($client);
check('start-async started', 1, \count($fake->of('StartWorkflowExecution')));
check('start-async no wait', 0, \count($fake->of('GetWorkflowExecutionHistory')));

// WorkflowRun details
$fake->reset([$confirmation]);
ob_start();
$moneyTransfer->runDetails($client, $client->newWorkflowStub(MoneyTransferWorkflowInterface::class));
$output = ob_get_clean();
$workflowId = $fake->of('StartWorkflowExecution')[0]->getWorkflowId();
check('run-details id', true, str_starts_with($output, "string(36) \"{$workflowId}\""));
check('run-details describe', $workflowId, $fake->of('DescribeWorkflowExecution')[0]->getExecution()->getWorkflowId());
check('run-details result', true, str_contains($output, $confirmation));

// Task Queue and Workflow ID
$fake->reset();
$client->start($examples->taskQueue($client), 'fromID', 'toID', 'refID', 1000);
check('task-queue', 'Workflow-Task-Queue-1', $fake->of('StartWorkflowExecution')[0]->getTaskQueue()->getName());
$fake->reset();
$client->start($examples->workflowId($client), 'fromID', 'toID', 'refID', 1000);
check('workflow-id', 'Workflow-Id', $fake->of('StartWorkflowExecution')[0]->getWorkflowId());

// Get Workflow results, typed and untyped
foreach (['getResult' => 'YourWorkflowInterface', 'getUntypedResult' => 'account.transfer'] as $method => $type) {
    $fake->reset(['done']);
    ob_start();
    $examples->$method($client);
    $output = ob_get_clean();
    check("{$method} type", $type, $fake->of('StartWorkflowExecution')[0]->getWorkflowType()->getName());
    check("{$method} output", "string(4) \"done\"\n", $output);
}

// Connection
$fake->reset();
$examples->connect($serviceClient);
check('connection capabilities', 1, \count($fake->of('GetSystemInfo')));

exit($failures > 0 ? 1 : 0);
