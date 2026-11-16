<?php
declare(strict_types=1);

namespace App\Activities\Timeouts;

class GreetingActivity implements GreetingActivityInterface
{
    public function composeGreeting(string $greeting, string $name): string
    {
        $name === '' and throw new \InvalidArgumentException('Name must not be empty');
        return $greeting . ' ' . $name;
    }
}
