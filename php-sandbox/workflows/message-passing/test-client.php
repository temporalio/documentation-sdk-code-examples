<?php
declare(strict_types=1);

// Runs the Client code from the Workflow message passing page against a fake
// Temporal Service, without the grpc extension. A fake gRPC stub stands in for
// the connection, and an interceptor records each request the SDK sends and
// returns a canned response. The Workflow side is tested by test.sh.
// Run from php-sandbox/:
//   php -d zend.assertions=1 workflows/message-passing/test-client.php

namespace App\Workflows\MessagePassing\Client {
    // The Query example waits 60 seconds between Queries. Skip the wait.
    function sleep(int $seconds): int
    {
        return 0;
    }
}

namespace {
    use App\Workflows\MessagePassing\Client\MessagePassingClient;
    use Ramsey\Uuid\UuidInterface;
    use Temporal\Api\Common\V1\WorkflowExecution;
    use Temporal\Api\Enums\V1\EventType;
    use Temporal\Api\Enums\V1\UpdateWorkflowExecutionLifecycleStage as Stage;
    use Temporal\Api\Enums\V1\WorkflowIdConflictPolicy;
    use Temporal\Api\History\V1\History;
    use Temporal\Api\History\V1\HistoryEvent;
    use Temporal\Api\History\V1\WorkflowExecutionCompletedEventAttributes;
    use Temporal\Api\Update\V1\Outcome;
    use Temporal\Api\Update\V1\UpdateRef;
    use Temporal\Api\Workflowservice\V1 as WS;
    use Temporal\Client\GRPC\ContextInterface;
    use Temporal\Client\GRPC\ServiceClient;
    use Temporal\Client\WorkflowClient;
    use Temporal\Client\WorkflowOptions;
    use Temporal\DataConverter\DataConverter;
    use Temporal\DataConverter\EncodedValues;
    use Temporal\Interceptor\GrpcClientInterceptor;
    use Temporal\Internal\Interceptor\Pipeline;

    require __DIR__ . '/../../vendor/autoload.php';

    final class FakeService implements GrpcClientInterceptor
    {
        /** @var list<array{string, object}> Every request, in order */
        public array $requests = [];
        /** @var list<mixed> Values to return, in order, as Workflow, Query, and Update results */
        public array $results = [];
        /** Lifecycle stage UpdateWorkflowExecution reports */
        public int $updateStage = Stage::UPDATE_WORKFLOW_EXECUTION_LIFECYCLE_STAGE_COMPLETED;

        public function interceptCall(string $method, object $arg, ContextInterface $ctx, callable $next): object
        {
            $this->requests[] = [$method, $arg];
            return match ($method) {
                'StartWorkflowExecution' => (new WS\StartWorkflowExecutionResponse())->setRunId('run-id')->setStarted(true),
                'SignalWithStartWorkflowExecution' => (new WS\SignalWithStartWorkflowExecutionResponse())->setRunId('run-id')->setStarted(true),
                'GetWorkflowExecutionHistory' => (new WS\GetWorkflowExecutionHistoryResponse())->setHistory(
                    (new History())->setEvents([
                        (new HistoryEvent())
                            ->setEventId(1)
                            ->setEventType(EventType::EVENT_TYPE_WORKFLOW_EXECUTION_COMPLETED)
                            ->setWorkflowExecutionCompletedEventAttributes(
                                (new WorkflowExecutionCompletedEventAttributes())->setResult($this->nextPayloads()),
                            ),
                    ]),
                ),
                'QueryWorkflow' => (new WS\QueryWorkflowResponse())->setQueryResult($this->nextPayloads()),
                'UpdateWorkflowExecution' => $this->updateResponse($arg->getRequest()->getMeta()->getUpdateId()),
                'PollWorkflowExecutionUpdate' => (new WS\PollWorkflowExecutionUpdateResponse())
                    ->setStage(Stage::UPDATE_WORKFLOW_EXECUTION_LIFECYCLE_STAGE_COMPLETED)
                    ->setOutcome((new Outcome())->setSuccess($this->nextPayloads())),
                'ExecuteMultiOperation' => (new WS\ExecuteMultiOperationResponse())->setResponses([
                    (new WS\ExecuteMultiOperationResponse\Response())->setStartWorkflow(
                        (new WS\StartWorkflowExecutionResponse())->setRunId('run-id')->setStarted(true),
                    ),
                    (new WS\ExecuteMultiOperationResponse\Response())->setUpdateWorkflow(
                        $this->updateResponse($arg->getOperations()[1]->getUpdateWorkflow()->getRequest()->getMeta()->getUpdateId()),
                    ),
                ]),
                default => new ("Temporal\\Api\\Workflowservice\\V1\\{$method}Response")(),
            };
        }

        /** The last request of a given RPC method */
        public function last(string $method): object
        {
            foreach (\array_reverse($this->requests) as [$m, $arg]) {
                if ($m === $method) {
                    return $arg;
                }
            }
            throw new \RuntimeException("No {$method} request");
        }

        public function reset(array $results): void
        {
            $this->requests = [];
            $this->results = $results;
            $this->updateStage = Stage::UPDATE_WORKFLOW_EXECUTION_LIFECYCLE_STAGE_COMPLETED;
        }

        private function updateResponse(string $updateId): WS\UpdateWorkflowExecutionResponse
        {
            $response = (new WS\UpdateWorkflowExecutionResponse())
                ->setUpdateRef((new UpdateRef())->setUpdateId($updateId)->setWorkflowExecution(
                    (new WorkflowExecution())->setWorkflowId('workflow-id')->setRunId('run-id'),
                ))
                ->setStage($this->updateStage);
            if ($this->updateStage === Stage::UPDATE_WORKFLOW_EXECUTION_LIFECYCLE_STAGE_COMPLETED) {
                $response->setOutcome((new Outcome())->setSuccess($this->nextPayloads()));
            }
            return $response;
        }

        private function nextPayloads(): \Temporal\Api\Common\V1\Payloads
        {
            return EncodedValues::fromValues([\array_shift($this->results)], DataConverter::createDefault())->toPayloads();
        }
    }

    \assert(\ini_get('zend.assertions') === '1') or print("Run with -d zend.assertions=1 so the examples' assert() calls run\n");

    $fake = new FakeService();
    $serviceClient = (new ServiceClient(static fn() => new class extends \Grpc\BaseStub {
        public function __construct() {}

        public function close(): void {}
    }))->withInterceptorPipeline(Pipeline::prepare([$fake]));
    $client = new WorkflowClient($serviceClient);
    $examples = new MessagePassingClient();
    $decode = static fn($payloads): array => EncodedValues::fromPayloads($payloads, DataConverter::createDefault())->getValues();

    $failures = 0;
    function check(string $name, mixed $expected, mixed $actual): void
    {
        global $failures;
        if ($expected === $actual) {
            echo "ok   {$name}\n";
        } else {
            echo "FAIL {$name}: expected " . \var_export($expected, true) . ', got ' . \var_export($actual, true) . "\n";
            $failures++;
        }
    }

    // Send a Signal from a Temporal Client
    $fake->reset(['OK']);
    $examples->sendSignal($client);
    check('send-signal start type', 'YourWorkflow', $fake->last('StartWorkflowExecution')->getWorkflowType()->getName());
    $signal = $fake->last('SignalWorkflowExecution');
    check('send-signal name', 'setValue', $signal->getSignalName());
    check('send-signal args', [true], $decode($signal->getInput()));
    check('send-signal same Workflow', $fake->last('StartWorkflowExecution')->getWorkflowId(), $signal->getWorkflowExecution()->getWorkflowId());

    $fake->reset([]);
    $examples->sendSignalToRunningWorkflow($client);
    $signal = $fake->last('SignalWorkflowExecution');
    check('send-signal-running id', 'workflowID', $signal->getWorkflowExecution()->getWorkflowId());
    check('send-signal-running name', 'setValue', $signal->getSignalName());

    // Signal-With-Start
    $fake->reset([]);
    $examples->signalWithStart($client);
    $request = $fake->last('SignalWithStartWorkflowExecution');
    check('signal-with-start type', 'YourWorkflow', $request->getWorkflowType()->getName());
    check('signal-with-start name', 'setValue', $request->getSignalName());
    check('signal-with-start args', [true], $decode($request->getSignalInput()));

    // Query from a Client
    $fake->reset(['waiting timer', 'done']);
    \ob_start();
    $examples->query($client);
    $output = \ob_get_clean();
    check('query type', 'current_state', $fake->last('QueryWorkflow')->getQuery()->getQueryType());
    check('query output', "string(13) \"waiting timer\"\nstring(4) \"done\"\n", $output);

    // Send an Update from a Client
    $fake->reset([1]);
    $count = $examples->sendUpdate($client, WorkflowOptions::new());
    $update = $fake->last('UpdateWorkflowExecution')->getRequest();
    check('send-update name', 'addGreeting', $update->getInput()->getName());
    check('send-update args', ['World'], $decode($update->getInput()->getArgs()));
    check('send-update result', 1, $count);

    // Async accept with startUpdate
    $fake->reset([]);
    $fake->updateStage = Stage::UPDATE_WORKFLOW_EXECUTION_LIFECYCLE_STAGE_ACCEPTED;
    $uuid = '0195f0a4-4b5e-7d0e-8a5f-7a1b2c3d4e5f';
    $fake->results = [1, $uuid];
    [$result, $resultUuid] = $examples->startUpdate($client, WorkflowOptions::new());
    check('start-update type', 'GreetingWorkflow', $fake->last('StartWorkflowExecution')->getWorkflowType()->getName());
    $updates = \array_values(\array_filter($fake->requests, fn($r) => $r[0] === 'UpdateWorkflowExecution'));
    check('start-update first name', 'addGreeting', $updates[0][1]->getRequest()->getInput()->getName());
    check('start-update second name', 'storeGreetings', $updates[1][1]->getRequest()->getInput()->getName());
    check('start-update result', 1, $result);
    check('start-update uuid type', true, $resultUuid instanceof UuidInterface);
    check('start-update uuid', $uuid, (string) $resultUuid);

    // Update-With-Start
    foreach (['updateWithStart', 'updateWithStartAndWait'] as $method) {
        $fake->reset([30]);
        $price = $examples->$method($client, 'cart-1', 'apple', 3);
        $ops = $fake->last('ExecuteMultiOperation')->getOperations();
        $start = $ops[0]->getStartWorkflow();
        $update = $ops[1]->getUpdateWorkflow()->getRequest();
        check("{$method} type", 'ShoppingCartWorkflow', $start->getWorkflowType()->getName());
        check("{$method} id", 'cart-1', $start->getWorkflowId());
        check("{$method} task queue", 'service-queue', $start->getTaskQueue()->getName());
        check("{$method} conflict policy", WorkflowIdConflictPolicy::WORKFLOW_ID_CONFLICT_POLICY_USE_EXISTING, $start->getWorkflowIdConflictPolicy());
        check("{$method} update name", 'addItem', $update->getInput()->getName());
        check("{$method} update args", ['apple', 3], $decode($update->getInput()->getArgs()));
        check("{$method} price", 30, $price);
    }

    exit($failures > 0 ? 1 : 0);
}
