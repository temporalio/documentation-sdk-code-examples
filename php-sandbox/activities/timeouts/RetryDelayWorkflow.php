<?php
declare(strict_types=1);

namespace App\Activities\Timeouts;

use Temporal\Activity\ActivityOptions;
use Temporal\Common\RetryOptions;
use Temporal\Workflow;

#[Workflow\WorkflowInterface]
class RetryDelayWorkflow
{
    #[Workflow\WorkflowMethod]
    public function run()
    {
        // The Retry Policy waits 60 seconds between attempts. The Activity's
        // nextRetryDelay (3 and 6 seconds) replaces that interval.
        return yield Workflow::executeActivity(
            'flaky',
            [],
            ActivityOptions::new()
                ->withStartToCloseTimeout(10)
                ->withRetryOptions(RetryOptions::new()->withInitialInterval(60)->withMaximumAttempts(5)),
        );
    }
}
