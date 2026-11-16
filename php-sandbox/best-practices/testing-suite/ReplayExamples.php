<?php
declare(strict_types=1);

namespace App\BestPractices\TestingSuite;

use Temporal\Client\WorkflowClientInterface;
use Temporal\Testing\Replay\WorkflowReplayer;
use Temporal\Workflow\WorkflowRunInterface;

// The replay examples from the Testing page. The WorkflowReplayer talks to the
// running RoadRunner server over its RPC port.
final class ReplayExamples
{
    public function __construct(
        private readonly WorkflowClientInterface $workflowClient,
    ) {}

    public function replayFromServer(WorkflowClientInterface $workflowClient, WorkflowReplayer $replayer): void
    {
        // @@@SNIPSTART php-testing-replay-from-server
        /**
         * We assume you already have a WorkflowClient and WorkflowReplayer in scope.
         * @var \Temporal\Client\WorkflowClientInterface $workflowClient
         * @var \Temporal\Testing\Replay\WorkflowReplayer $replayer
         */

        // Find all workflow executions of type "MyWorkflow" and task queue "MyTaskQueue".
        $executions = $workflowClient->listWorkflowExecutions(
            "WorkflowType='MyWorkflow' AND TaskQueue='MyTaskQueue'"
        );

        // Replay each workflow execution.
        foreach ($executions as $executionInfo) {
            try {
                $replayer->replayFromServer(
                    workflowType: $executionInfo->type->name,
                    execution: $executionInfo->execution,
                );
            } catch (\Temporal\Testing\Replay\Exception\ReplayerException $e) {
                // Handle a replay error.
            }
        }
        // @@@SNIPEND
    }

    public function replayFromJson(WorkflowReplayer $replayer): void
    {
        // @@@SNIPSTART php-testing-replay-from-json
        $replayer->replayFromJSON(
            workflowType: 'MyWorkflow',
            path: 'history.json',
            lastEventId: 42,  // optional
        );
        // @@@SNIPEND
    }

    public function replayDownloadedHistory(WorkflowRunInterface $run): void
    {
        // @@@SNIPSTART php-testing-replay-history
        $history = $this->workflowClient->getWorkflowHistory(
            execution: $run->getExecution(),
        )->getHistory();

        (new WorkflowReplayer())->replayHistory($history);
        // @@@SNIPEND
    }
}
