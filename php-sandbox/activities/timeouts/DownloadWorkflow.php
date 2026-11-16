<?php
declare(strict_types=1);

namespace App\Activities\Timeouts;

use Temporal\Activity\ActivityOptions;
use Temporal\Common\RetryOptions;
use Temporal\Workflow;

#[Workflow\WorkflowInterface]
class DownloadWorkflow
{
    #[Workflow\WorkflowMethod]
    public function run()
    {
        // The download takes about 5 seconds. Heartbeats every second keep it
        // alive past the 2-second Heartbeat Timeout.
        $activities = Workflow::newActivityStub(
            FileProcessingActivities::class,
            ActivityOptions::new()
                ->withStartToCloseTimeout(30)
                ->withHeartbeatTimeout(2)
                ->withRetryOptions(RetryOptions::new()->withMaximumAttempts(1)),
        );

        yield $activities->download('my-bucket', 'report.txt', 'report.txt');
        return 'downloaded';
    }
}
