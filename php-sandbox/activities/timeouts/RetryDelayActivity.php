<?php
declare(strict_types=1);

namespace App\Activities\Timeouts;

use Temporal\Activity\ActivityInterface;
use Temporal\Activity\ActivityMethod;

#[ActivityInterface]
class RetryDelayActivity
{
    // Fails on the first two attempts, each time asking for a retry delay of
    // three seconds times the attempt number.
    #[ActivityMethod]
    public function flaky(): int
    {
        if (\Temporal\Activity::getInfo()->attempt >= 3) {
            return \Temporal\Activity::getInfo()->attempt;
        }

        // @@@SNIPSTART php-activity-timeouts-next-retry-delay
        $attempt = \Temporal\Activity::getInfo()->attempt;

        throw new \Temporal\Exception\Failure\ApplicationFailure(
            message: "Something bad happened on attempt $attempt",
            type: 'my_failure_type',
            nonRetryable: false,
            nextRetryDelay: \DateInterval::createFromDateString(\sprintf('%d seconds', $attempt * 3)),
        );
        // @@@SNIPEND
    }
}
