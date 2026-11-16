<?php
declare(strict_types=1);

namespace App\Platform\Observability;

use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
interface OrderWorkflowInterface
{
    #[WorkflowMethod]
    public function run();
}
