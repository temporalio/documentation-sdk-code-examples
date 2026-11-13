<?php
declare(strict_types=1);

namespace App\Workflows\ContinueAsNew;

use Temporal\Exception\Failure\ApplicationFailure;
use Temporal\Workflow;

class MessageHandlerWorkflow implements MessageHandlerWorkflowInterface
{
    private ClusterManagerState $state;
    private ?int $maxHistoryLength;

    #[Workflow\WorkflowInit]
    public function __construct(ClusterManagerInput $input)
    {
        $this->state = $input->state ?? new ClusterManagerState();
        $this->maxHistoryLength = $input->testContinueAsNew ? 50 : null;
    }

    public function run(ClusterManagerInput $input)
    {
        yield Workflow::await(fn() => $this->state->clusterStarted);

        while (true) {
            yield Workflow::await(fn() => $this->state->clusterShutdown || $this->shouldContinueAsNew());

            if ($this->state->clusterShutdown) {
                break;
            }

            // Don't leave Update handlers half-finished when you Continue-As-New.
            yield Workflow::await(fn() => Workflow::allHandlersFinished());
            // @@@SNIPSTART php-continue-as-new-call
            return yield Workflow::continueAsNew(
                Workflow::getInfo()->type->name,
                [new ClusterManagerInput($this->state, $input->testContinueAsNew)],
            );
            // @@@SNIPEND
        }

        yield Workflow::await(fn() => Workflow::allHandlersFinished());
        return \count($this->getAssignedNodes());
    }

    public function startCluster(): void
    {
        $this->state->clusterStarted = true;
        $this->state->nodes = \array_fill_keys(\range(0, 24), null);
    }

    public function shutdownCluster(): void
    {
        $this->state->clusterShutdown = true;
    }

    public function assignNodesToJob(string $jobName, int $numNodes)
    {
        yield Workflow::await(fn() => $this->state->clusterStarted);
        $this->state->clusterShutdown and throw new ApplicationFailure(
            'Cannot assign nodes to a job: Cluster is already shut down', 'CannotAssignNodesToJob', true,
        );

        // Idempotency guard.
        if (\in_array($jobName, $this->state->jobsAssigned, true)) {
            return $this->getAssignedNodes($jobName);
        }

        $unassignedNodes = \array_keys(\array_filter($this->state->nodes, fn($v) => $v === null));
        \count($unassignedNodes) < $numNodes and throw new ApplicationFailure(
            \sprintf('Cannot assign %d nodes; have only %d available', $numNodes, \count($unassignedNodes)),
            'CannotAssignNodesToJob',
            true,
        );

        foreach (\array_slice($unassignedNodes, 0, $numNodes) as $node) {
            $this->state->nodes[$node] = $jobName;
        }
        $this->state->jobsAssigned[] = $jobName;

        return $this->getAssignedNodes($jobName);
    }

    public function getState(): ClusterManagerState
    {
        return $this->state;
    }

    private function getAssignedNodes(?string $jobName = null): array
    {
        return $jobName === null
            ? \array_keys(\array_filter($this->state->nodes, fn($v) => $v !== null))
            : \array_keys(\array_filter($this->state->nodes, fn($v) => $v === $jobName));
    }

    // @@@SNIPSTART php-continue-as-new-should-continue-as-new
    private function shouldContinueAsNew(): bool
    {
        if (Workflow::getInfo()->shouldContinueAsNew) {
            return true;
        }

        // This is just for ease-of-testing.  In production, we trust temporal to tell us when to continue as new.
        if ($this->maxHistoryLength !== null && Workflow::getInfo()->historyLength > $this->maxHistoryLength) {
            return true;
        }

        return false;
    }
    // @@@SNIPEND
}
