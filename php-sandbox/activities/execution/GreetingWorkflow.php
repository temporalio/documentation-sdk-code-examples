<?php
declare(strict_types=1);

namespace App\Activities\Execution;

use App\Activities\Basics\ReturnPrimitive\GreetingActivityInterface;
use Temporal\Activity\ActivityOptions;
use Temporal\Workflow;

// @@@SNIPSTART php-activity-execution-get-result
class GreetingWorkflow implements GreetingWorkflowInterface
{
    private $greetingActivity;

    public function __construct()
    {
        $this->greetingActivity = Workflow::newActivityStub(
            GreetingActivityInterface::class,
            ActivityOptions::new()->withStartToCloseTimeout(\DateInterval::createFromDateString('30 seconds'))
        );
    }

    public function greet(string $name): \Generator
    {
        // This is a blocking call that returns only after the activity has completed.
        return yield $this->greetingActivity->composeGreeting('Hello', $name);
    }
}
// @@@SNIPEND
