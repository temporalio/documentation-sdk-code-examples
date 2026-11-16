<?php
declare(strict_types=1);

namespace App\Activities\AsynchronousActivity;

use Temporal\Activity\ActivityOptions;
use Temporal\Workflow;

#[Workflow\WorkflowInterface]
class AsyncGreetingWorkflow
{
    #[Workflow\WorkflowMethod]
    public function greet(string $name)
    {
        $activity = Workflow::newActivityStub(
            GreetingActivityInterface::class,
            ActivityOptions::new()->withStartToCloseTimeout(60),
        );

        return yield $activity->composeGreeting('Hello', $name);
    }
}
