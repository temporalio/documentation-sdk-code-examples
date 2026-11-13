<?php
declare(strict_types=1);

namespace App\Workflows\Schedules;

use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
interface CronWorkflowInterface
{
    public const WORKFLOW_ID = 'cron-workflow';

    #[WorkflowMethod]
    public function greet(string $name);
}
