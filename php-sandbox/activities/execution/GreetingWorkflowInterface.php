<?php
declare(strict_types=1);

namespace App\Activities\Execution;

use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
interface GreetingWorkflowInterface
{
    #[WorkflowMethod]
    public function greet(string $name);
}
