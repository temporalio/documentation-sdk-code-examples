<?php
declare(strict_types=1);

namespace App\Activities\Basics\ReturnPrimitive;

use Temporal\Activity\ActivityInterface;

#[ActivityInterface]
interface GreetingActivityInterface
{
    public function composeGreeting(string $greeting, string $name): string;
}
