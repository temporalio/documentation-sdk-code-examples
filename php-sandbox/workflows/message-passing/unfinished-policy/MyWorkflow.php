<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\UnfinishedPolicy;

use Temporal\Workflow;
use Temporal\Workflow\HandlerUnfinishedPolicy;
use Temporal\Workflow\UpdateMethod;
use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
class MyWorkflow
{
    #[WorkflowMethod]
    public function run()
    {
        // Return without waiting for handlers to finish.
        return 'workflow-result';
    }

    // @@@SNIPSTART php-message-passing-unfinished-policy
    #[UpdateMethod(unfinishedPolicy: HandlerUnfinishedPolicy::Abandon)]
    public function myUpdate()
    {
        // ...
        yield Workflow::timer(2);
    }
    // @@@SNIPEND
}
