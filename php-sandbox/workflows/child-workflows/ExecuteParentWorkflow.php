<?php
declare(strict_types=1);

namespace App\Workflows\ChildWorkflows;

use Temporal\DataConverter\Type;
use Temporal\Workflow;
use Temporal\Workflow\ChildWorkflowOptions;

#[Workflow\WorkflowInterface]
class ExecuteParentWorkflow
{
    #[Workflow\WorkflowMethod]
    #[Workflow\ReturnType('string')]
    public function run()
    {
        // @@@SNIPSTART php-child-workflows-execute
        $childResult = yield Workflow::executeChildWorkflow(
            'ChildWorkflowName',
            ['args'],
            ChildWorkflowOptions::new()->withWorkflowId('BID-SIMPLE-CHILD-WORKFLOW'),
            Type::TYPE_STRING // optional: defines the return type
        );
        // @@@SNIPEND

        return $childResult;
    }
}
