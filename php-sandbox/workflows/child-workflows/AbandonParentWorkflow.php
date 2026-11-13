<?php
declare(strict_types=1);

namespace App\Workflows\ChildWorkflows;

use Temporal\Workflow;
use Temporal\Workflow\ChildWorkflowOptions;
use Temporal\Workflow\ParentClosePolicy;

#[Workflow\WorkflowInterface]
class AbandonParentWorkflow
{
    #[Workflow\WorkflowMethod]
    public function run()
    {
        // @@@SNIPSTART php-child-workflows-parent-close-policy
        $child = Workflow::newUntypedChildWorkflowStub(
            'child-workflow',
            ChildWorkflowOptions::new()
                ->withParentClosePolicy(ParentClosePolicy::POLICY_ABANDON)
        );

        yield $child->start();
        // @@@SNIPEND
    }
}
