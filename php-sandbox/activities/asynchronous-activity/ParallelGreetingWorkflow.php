<?php
declare(strict_types=1);

namespace App\Activities\AsynchronousActivity;

use App\Activities\Basics\ReturnPrimitive\GreetingActivityInterface;
use Temporal\Activity\ActivityOptions;
use Temporal\Workflow;

#[Workflow\WorkflowInterface]
class ParallelGreetingWorkflow
{
    private $greetingActivity;

    public function __construct()
    {
        $this->greetingActivity = Workflow::newActivityStub(
            GreetingActivityInterface::class,
            ActivityOptions::new()->withStartToCloseTimeout(10),
        );
    }

    // @@@SNIPSTART php-async-activity-parallel
    #[Workflow\WorkflowMethod]
    public function greet(string $name): \Generator
    {
        // Workflow::async runs it's activities and child workflows in a separate coroutine. Use keyword yield to merge
        // it back to parent process.

        $first = Workflow::async(
            function () use ($name) {
                $hello = yield $this->greetingActivity->composeGreeting('Hello', $name);
                $bye = yield $this->greetingActivity->composeGreeting('Bye', $name);

                return $hello . '; ' . $bye;
            }
        );

        $second = Workflow::async(
            function () use ($name) {
                $hello = yield $this->greetingActivity->composeGreeting('Hola', $name);
                $bye = yield $this->greetingActivity->composeGreeting('Chao', $name);

                return $hello . '; ' . $bye;
            }
        );

        // blocks until $first and $second complete
        return (yield $first) . "\n" . (yield $second);
    }
    // @@@SNIPEND
}
