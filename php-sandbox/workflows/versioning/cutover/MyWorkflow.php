<?php // @@@SNIPSTART php-versioning-cutover-workflows ?>
<?php
declare(strict_types=1);

namespace App\Workflows\Versioning\Cutover;

use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
class MyWorkflow
{
    #[WorkflowMethod]
    public function runAsync()
    {
        // ...
        return 'v1';
    }
}

#[WorkflowInterface]
class MyWorkflowV2
{
    #[WorkflowMethod]
    public function runAsync()
    {
        // ...
        return 'v2';
    }
}
// @@@SNIPEND
