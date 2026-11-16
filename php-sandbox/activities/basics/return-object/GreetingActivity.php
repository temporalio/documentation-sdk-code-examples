<?php
declare(strict_types=1);

namespace App\Activities\Basics\ReturnObject;

// @@@SNIPSTART php-activity-basics-return-object
class GreetingActivity implements GreetingActivityInterface
{
    public function composeGreeting(string $greeting, string $name): Greeting
    {
        return new Greeting($greeting, $name);
    }
}
// @@@SNIPEND
