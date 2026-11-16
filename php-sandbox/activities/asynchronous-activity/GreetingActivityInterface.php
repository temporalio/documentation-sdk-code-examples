<?php
declare(strict_types=1);

namespace App\Activities\AsynchronousActivity;

use Temporal\Activity\ActivityInterface;
use Temporal\Activity\ActivityMethod;

#[ActivityInterface(prefix: 'AsyncActivityCompletion.')]
interface GreetingActivityInterface
{
    #[ActivityMethod]
    public function composeGreeting(string $greeting, string $name): string;
}
