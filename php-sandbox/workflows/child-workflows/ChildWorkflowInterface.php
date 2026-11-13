<?php
declare(strict_types=1);

namespace App\Workflows\ChildWorkflows;

use Temporal\Workflow\ReturnType;
use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
interface ChildWorkflowInterface
{
    #[WorkflowMethod(name: 'ChildWorkflowName')]
    #[ReturnType('string')]
    public function workflowMethod(string $value);
}
