<?php
declare(strict_types=1);

namespace App\Activities\Timeouts;

use Temporal\Activity\ActivityInterface;
use Temporal\Activity\ActivityMethod;

#[ActivityInterface(prefix: 'Timeouts.')]
interface GreetingActivityInterface
{
    #[ActivityMethod]
    public function composeGreeting(string $greeting, string $name): string;
}
