<?php
declare(strict_types=1);

namespace App\Workflows\Schedules;

use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
interface GreeterWorkflowInterface
{
    #[WorkflowMethod]
    public function greet(string $greeting);
}
