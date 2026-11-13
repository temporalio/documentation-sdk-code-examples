<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\SignalFromWorkflow;

use Temporal\Workflow;
use Temporal\Workflow\WorkflowExecution;

#[Workflow\WorkflowInterface]
class UntypedSignalSenderWorkflow
{
    #[Workflow\WorkflowMethod]
    public function run(string $workflowId)
    {
        // @@@SNIPSTART php-message-passing-send-untyped-signal-from-workflow
        $workflow = Workflow::newUntypedExternalWorkflowStub(new WorkflowExecution($workflowId));

        yield $workflow->signal('setValue', [true]);
        // @@@SNIPEND
    }
}
