<?php // @@@SNIPSTART php-continue-as-new-interface ?>
<?php
declare(strict_types=1);

namespace App\Workflows\ContinueAsNew;

use Temporal\Workflow;

#[Workflow\WorkflowInterface]
interface MessageHandlerWorkflowInterface
{
    #[Workflow\WorkflowMethod]
    #[Workflow\ReturnType('int')]
    public function run(ClusterManagerInput $input);

    #[Workflow\SignalMethod]
    public function startCluster(): void;

    #[Workflow\SignalMethod]
    public function shutdownCluster(): void;

    #[Workflow\UpdateMethod]
    public function assignNodesToJob(string $jobName, int $numNodes);

    #[Workflow\QueryMethod]
    public function getState(): ClusterManagerState;
}
// @@@SNIPEND
