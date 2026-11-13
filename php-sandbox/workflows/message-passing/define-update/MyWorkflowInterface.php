<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\DefineUpdate;

use Temporal\Workflow\UpdateMethod;
use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
interface MyWorkflowInterface
{
    #[WorkflowMethod]
    public function run();

    // @@@SNIPSTART php-message-passing-define-update
    #[UpdateMethod]
    public function myUpdate(string $value);
    // @@@SNIPEND
}
