<?php
declare(strict_types=1);

namespace App\Workflows\Timeouts;

use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
interface MyWorkflowInterface
{
    #[WorkflowMethod]
    public function run();
}
