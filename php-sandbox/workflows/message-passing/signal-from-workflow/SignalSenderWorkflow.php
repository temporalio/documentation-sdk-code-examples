<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\SignalFromWorkflow;

use App\Workflows\MessagePassing\HandleSignal\YourWorkflow;
use Temporal\Workflow;
use Temporal\Workflow\WorkflowExecution;

#[Workflow\WorkflowInterface]
class SignalSenderWorkflow
{
    #[Workflow\WorkflowMethod]
    public function run(string $workflowId)
    {
        // @@@SNIPSTART php-message-passing-send-signal-from-workflow
        $workflow = Workflow::newExternalWorkflowStub(
            YourWorkflow::class,
            new WorkflowExecution($workflowId),
        );

        yield $workflow->setValue(true);
        // @@@SNIPEND
    }
}
