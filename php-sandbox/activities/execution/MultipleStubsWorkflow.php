<?php
declare(strict_types=1);

namespace App\Activities\Execution;

use App\Activities\Basics\ReturnPrimitive\GreetingActivityInterface;
use Temporal\Activity\ActivityOptions;
use Temporal\Workflow;

#[Workflow\WorkflowInterface]
class MultipleStubsWorkflow
{
    #[Workflow\WorkflowMethod]
    public function greet(string $name)
    {
        // @@@SNIPSTART php-activity-execution-multiple-stubs
        $greetingActivity = Workflow::newActivityStub(
            GreetingActivityInterface::class,
            ActivityOptions::new()->withStartToCloseTimeout(\DateInterval::createFromDateString('30 seconds'))
        );

        $slowGreetingActivity = Workflow::newActivityStub(
            GreetingActivityInterface::class,
            ActivityOptions::new()->withStartToCloseTimeout(\DateInterval::createFromDateString('30 minutes'))
        );
        // @@@SNIPEND

        return [
            yield $greetingActivity->composeGreeting('Hello', $name),
            yield $slowGreetingActivity->composeGreeting('Goodbye', $name),
        ];
    }
}
