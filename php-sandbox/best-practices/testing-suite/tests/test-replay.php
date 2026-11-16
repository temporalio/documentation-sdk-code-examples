<?php
declare(strict_types=1);

// Runs the replay examples from the Testing page against the RoadRunner server
// test.sh starts. The WorkflowReplayer is real. The examples' WorkflowClient is
// backed by a fake Temporal Service (no grpc extension needed) that answers
// with real data: the Executions and Event History that test.sh exported from
// the dev server with the Temporal CLI.
//   RR_RPC=tcp://127.0.0.1:6099 php test-replay.php <dir with executions.json and history.json>

use App\BestPractices\TestingSuite\ReplayExamples;
use Temporal\Api\Common\V1\WorkflowExecution;
use Temporal\Api\Common\V1\WorkflowType;
use Temporal\Api\Enums\V1\WorkflowExecutionStatus;
use Temporal\Api\History\V1\History;
use Temporal\Api\Workflow\V1\WorkflowExecutionInfo;
use Temporal\Api\Workflowservice\V1 as WS;
use Temporal\Client\GRPC\ContextInterface;
use Temporal\Client\GRPC\ServiceClient;
use Temporal\Client\WorkflowClient;
use Temporal\Client\WorkflowOptions;
use Temporal\Interceptor\GrpcClientInterceptor;
use Temporal\Internal\Interceptor\Pipeline;
use Temporal\Testing\Replay\Exception\ReplayerException;
use Temporal\Testing\Replay\WorkflowReplayer;

require __DIR__ . '/../../../vendor/autoload.php';

$dir = $argv[1];
/** @var list<array{workflowId: string, runId: string}> $executions */
$executions = json_decode(file_get_contents("$dir/executions.json"), true);
$history = new History();
$history->mergeFromJsonString(file_get_contents("$dir/history.json"), true);

$fake = new class($executions, $history) implements GrpcClientInterceptor {
    /** @var list<string> */
    public array $methods = [];

    public function __construct(private array $executions, private History $history) {}

    public function interceptCall(string $method, object $arg, ContextInterface $ctx, callable $next): object
    {
        $this->methods[] = $method;
        return match ($method) {
            'ListWorkflowExecutions' => (new WS\ListWorkflowExecutionsResponse())->setExecutions(\array_map(
                static fn(array $e) => (new WorkflowExecutionInfo())
                    ->setExecution((new WorkflowExecution())->setWorkflowId($e['workflowId'])->setRunId($e['runId']))
                    ->setType((new WorkflowType())->setName('MyWorkflow'))
                    ->setStatus(WorkflowExecutionStatus::WORKFLOW_EXECUTION_STATUS_COMPLETED),
                $this->executions,
            )),
            'StartWorkflowExecution' => (new WS\StartWorkflowExecutionResponse())
                ->setRunId($this->executions[0]['runId'])->setStarted(true),
            'GetWorkflowExecutionHistory' => (new WS\GetWorkflowExecutionHistoryResponse())->setHistory($this->history),
            default => new ("Temporal\\Api\\Workflowservice\\V1\\{$method}Response")(),
        };
    }
};
$client = new WorkflowClient((new ServiceClient(static fn() => new class extends \Grpc\BaseStub {
    public function __construct() {}

    public function close(): void {}
}))->withInterceptorPipeline(Pipeline::prepare([$fake])));
$examples = new ReplayExamples($client);
$replayer = new WorkflowReplayer();

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
$replays = static function (callable $replay): string {
    try {
        $replay();
        return 'ok';
    } catch (ReplayerException $e) {
        return 'failed';
    }
};

// Replay from the server: the example lists the Executions, and RoadRunner
// fetches each one's history from the dev server and replays it.
$examples->replayFromServer($client, $replayer);
check('replay-from-server listed', 1, count(array_keys($fake->methods, 'ListWorkflowExecutions')));
foreach ($executions as $e) {
    check("replay-from-server {$e['workflowId']}", 'ok', $replays(fn() => $replayer->replayFromServer(
        'MyWorkflow',
        new \Temporal\Workflow\WorkflowExecution($e['workflowId'], $e['runId']),
    )));
}

// Replay from a JSON file, up to Event 42. RoadRunner reads the file, so the
// relative path 'history.json' is resolved from RoadRunner's working directory
// (tests/), where test.sh copies it.
check('replay-from-json', 'ok', $replays(fn() => $examples->replayFromJson($replayer)));

// Replay a history downloaded with the WorkflowClient.
$run = $client->start($client->newWorkflowStub(
    \App\BestPractices\TestingSuite\SimpleWorkflow::class,
    WorkflowOptions::new()->withWorkflowId($executions[0]['workflowId']),
), 'hello');
check('replay-downloaded-history', 'ok', $replays(fn() => $examples->replayDownloadedHistory($run)));

// A history that doesn't match the Workflow Definition fails to replay.
$events = $history->getEvents();
foreach ($events as $event) {
    $attributes = $event->getActivityTaskScheduledEventAttributes();
    $attributes?->getActivityType()->setName('SimpleActivity.somethingElse');
}
check('replay-detects-nondeterminism', 'failed', $replays(fn() => $replayer->replayHistory($history)));

exit($failures > 0 ? 1 : 0);
