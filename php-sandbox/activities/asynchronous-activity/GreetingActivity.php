<?php
declare(strict_types=1);

namespace App\Activities\AsynchronousActivity;

use Psr\Log\LoggerInterface;
use Temporal\Activity;
use Temporal\Worker\Logger\StderrLogger;

// @@@SNIPSTART php-async-activity-complete-async
class GreetingActivity implements GreetingActivityInterface
{
    private LoggerInterface $logger;

    public function __construct()
    {
        $this->logger = new StderrLogger();
    }
    /**
     * Demonstrates how to implement an Activity asynchronously.
     * When {@link Activity::doNotCompleteOnReturn()} is called,
     * the Activity implementation function that returns doesn't complete the Activity.
     */
    public function composeGreeting(string $greeting, string $name): string
    {
        // In real life this request can be executed anywhere. By a separate service for example.
        $this->logger->info(sprintf('GreetingActivity token: %s', base64_encode(Activity::getInfo()->taskToken)));
        // Send the taskToken to the external service that will complete the Activity.
        // Return from the Activity a function indicating that Temporal should wait
        // for an async completion message.
        Activity::doNotCompleteOnReturn();
        // When doNotCompleteOnReturn() is invoked the return value is ignored.
        return 'ignored';
    }
}
// @@@SNIPEND
