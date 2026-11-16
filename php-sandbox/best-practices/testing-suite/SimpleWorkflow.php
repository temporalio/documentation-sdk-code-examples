<?php
declare(strict_types=1);

namespace App\BestPractices\TestingSuite;

use Temporal\Activity\ActivityOptions;
use Temporal\Common\RetryOptions;
use Temporal\Workflow;

#[Workflow\WorkflowInterface]
class SimpleWorkflow
{
    // The replay examples on the Testing page use the Workflow Type MyWorkflow.
    #[Workflow\WorkflowMethod(name: 'MyWorkflow')]
    #[Workflow\ReturnType('string')]
    public function run(string $input)
    {
        $activity = Workflow::newActivityStub(
            SimpleActivityInterface::class,
            ActivityOptions::new()
                ->withStartToCloseTimeout(5)
                ->withRetryOptions(RetryOptions::new()->withMaximumAttempts(1)),
        );

        return yield $activity->doSomething($input);
    }
}
