<?php
declare(strict_types=1);

namespace App\Workflows\Timers;

use Temporal\Workflow;

#[Workflow\WorkflowInterface]
class TimerWorkflow
{
    #[Workflow\WorkflowMethod]
    public function run()
    {
        // @@@SNIPSTART php-timers-sleep
        yield Workflow::timer(300); // sleep for 5 minutes
        // @@@SNIPEND

        return 'done';
    }
}
