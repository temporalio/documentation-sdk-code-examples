<?php
declare(strict_types=1);

namespace App\Activities\Timeouts;

use Carbon\CarbonInterval;
use Temporal\Activity\ActivityOptions;
use Temporal\Workflow;

#[Workflow\WorkflowInterface]
class TimeoutWorkflow
{
    private $greetingActivity;

    public function __construct()
    {
        // @@@SNIPSTART php-activity-timeouts-schedule-to-close
        $this->greetingActivity = Workflow::newActivityStub(
            GreetingActivityInterface::class,
            // Set Activity Timeout duration
            ActivityOptions::new()
                ->withScheduleToCloseTimeout(CarbonInterval::seconds(2))
                // ->withStartToCloseTimeout(CarbonInterval::seconds(2))
                // ->withScheduleToStartTimeout(CarbonInterval::seconds(10))
        );
        // @@@SNIPEND
    }

    #[Workflow\WorkflowMethod]
    public function greet(string $name)
    {
        return yield $this->greetingActivity->composeGreeting('Hello', $name);
    }
}
