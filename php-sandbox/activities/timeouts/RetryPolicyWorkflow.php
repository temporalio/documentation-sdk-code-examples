<?php
declare(strict_types=1);

namespace App\Activities\Timeouts;

use Carbon\CarbonInterval;
use Temporal\Activity\ActivityOptions;
use Temporal\Common\RetryOptions;
use Temporal\Exception\Failure\ActivityFailure;
use Temporal\Workflow;

#[Workflow\WorkflowInterface]
class RetryPolicyWorkflow
{
    private $greetingActivity;

    public function __construct()
    {
        // @@@SNIPSTART php-activity-timeouts-retry-policy
        $this->greetingActivity = Workflow::newActivityStub(
            GreetingActivityInterface::class,
            ActivityOptions::new()
                ->withScheduleToCloseTimeout(CarbonInterval::seconds(10))
                ->withRetryOptions(
                    RetryOptions::new()
                        ->withInitialInterval(CarbonInterval::seconds(1))
                        ->withMaximumAttempts(5)
                        ->withNonRetryableExceptions([\InvalidArgumentException::class])
                )
        );
        // @@@SNIPEND
    }

    #[Workflow\WorkflowMethod]
    public function greet(string $name)
    {
        try {
            return yield $this->greetingActivity->composeGreeting('Hello', $name);
        } catch (ActivityFailure $e) {
            return 'failed: ' . $e->getPrevious()?->getType();
        }
    }
}
