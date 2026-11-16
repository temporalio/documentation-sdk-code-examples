<?php
declare(strict_types=1);

namespace App\Activities\Basics;

use Temporal\Activity\ActivityOptions;
use Temporal\Common\RetryOptions;
use Temporal\Workflow;

// Test-only Workflow: calls each Activity Type by name on its Task Queue and
// returns what each call returned, or the error.
#[Workflow\WorkflowInterface]
class ActivityBasicsTestWorkflow
{
    /** @param array<string, list<mixed>> $calls Activity Type => arguments */
    #[Workflow\WorkflowMethod]
    public function run(array $calls)
    {
        $options = ActivityOptions::new()
            ->withStartToCloseTimeout(5)
            ->withRetryOptions(RetryOptions::new()->withMaximumAttempts(1));
        $results = [];
        foreach ($calls as $type => $args) {
            try {
                $results[$type] = yield Workflow::executeActivity($type, $args, $options);
            } catch (\Throwable $e) {
                $results[$type] = 'error: ' . ($e->getPrevious()?->getMessage() ?? $e->getMessage());
            }
        }
        return $results;
    }
}
