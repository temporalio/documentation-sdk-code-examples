<?php
declare(strict_types=1);

namespace App\Activities\Basics\ReturnPrimitive;

// @@@SNIPSTART php-activity-basics-return-primitive
class GreetingActivity implements GreetingActivityInterface
{
    public function composeGreeting(string $greeting, string $name): string
    {
        return $greeting . ' ' . $name;
    }
}
// @@@SNIPEND
