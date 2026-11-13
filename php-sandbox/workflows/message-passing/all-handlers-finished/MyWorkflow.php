<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\AllHandlersFinished;

use Temporal\Workflow;
use Temporal\Workflow\UpdateMethod;
use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
class MyWorkflow
{
    // @@@SNIPSTART php-message-passing-all-handlers-finished
    #[WorkflowMethod]
    public function run()
    {
        // ...
        yield Workflow::await(fn() => Workflow::allHandlersFinished());
        return "workflow-result";
    }
    // @@@SNIPEND

    // An Update that is still running when the main Workflow method reaches its end.
    #[UpdateMethod]
    public function slowUpdate()
    {
        yield Workflow::timer(2);
        return 'update-result';
    }
}
