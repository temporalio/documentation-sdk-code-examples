<?php
declare(strict_types=1);

namespace App\Workflows\ChildWorkflows;

use DateInterval;
use Temporal\Workflow;

// The Child Workflow that the Parent Close Policy example starts. It runs
// long enough to outlive its parent.
#[Workflow\WorkflowInterface]
class LongRunningChildWorkflow
{
    #[Workflow\WorkflowMethod(name: 'child-workflow')]
    public function run()
    {
        yield Workflow::timer(DateInterval::createFromDateString('1 hour'));
    }
}
