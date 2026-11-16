<?php
declare(strict_types=1);

namespace App\Activities\AsynchronousActivity;

use Temporal\Client\ActivityCompletionClientInterface;
use Temporal\Client\WorkflowClientInterface;

// Client code that completes or fails an asynchronously completed Activity.
final class ActivityCompletion
{
    /** @param string $token The base64-encoded Task Token that GreetingActivity logs */
    public function complete(WorkflowClientInterface $workflowClient, string $token, string $message): void
    {
        // @@@SNIPSTART php-async-activity-complete-by-token
        $client = $workflowClient->newActivityCompletionClient();
        // Complete the Activity.
        $client->completeByToken(
            base64_decode($token),
            $message
        );
        // @@@SNIPEND
    }

    public function fail(ActivityCompletionClientInterface $activityClient, string $taskToken): void
    {
        // @@@SNIPSTART php-async-activity-fail-by-token
        // Fail the Activity.
        $activityClient->completeExceptionallyByToken($taskToken, new \Error("activity failed"));
        // @@@SNIPEND
    }
}
