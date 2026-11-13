<?php
declare(strict_types=1);

namespace App\Workflows\ChildWorkflows;

class ChildWorkflow implements ChildWorkflowInterface
{
    public function workflowMethod(string $value)
    {
        return "Child received {$value}";
    }
}
