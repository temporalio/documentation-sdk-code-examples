<?php
declare(strict_types=1);

namespace App\Workflows\Cancellation;

use Temporal\Activity\ActivityCancellationType;
use Temporal\Activity\ActivityOptions;
use Temporal\Exception\Failure\ActivityFailure;
use Temporal\Exception\Failure\CanceledFailure;
use Temporal\Workflow;

#[Workflow\WorkflowInterface]
class CancelActivityWorkflow
{
    #[Workflow\WorkflowMethod]
    #[Workflow\ReturnType('string')]
    public function run()
    {
        // @@@SNIPSTART php-cancellation-cancel-activity
        $activity = Workflow::newActivityStub(
            MyActivityInterface::class,
            ActivityOptions::new()
                ->withStartToCloseTimeout('1 minute')
                // The Activity must heartbeat within this timeout to receive cancellation.
                ->withHeartbeatTimeout('5 seconds')
                // Wait for the Activity to finish its cleanup before the Workflow continues.
                ->withCancellationType(ActivityCancellationType::WaitCancellationCompleted),
        );

        // Start the Activity in its own cancellation scope.
        $scope = Workflow::async(function () use ($activity, &$promise) {
            $promise = $activity->processItems(60);
            return yield $promise;
        });

        // Let the Activity run for a while, then cancel it.
        yield Workflow::timer(3);
        $scope->cancel();

        try {
            // Wait on the Activity itself, not the scope. The scope rejects as soon as
            // it's canceled, but the Activity's promise waits for the Activity to
            // finish its cleanup.
            return yield $promise;
        } catch (ActivityFailure $e) {
            if (!$e->getPrevious() instanceof CanceledFailure) {
                throw $e;
            }
            return 'Activity canceled';
        }
        // @@@SNIPEND
    }
}
