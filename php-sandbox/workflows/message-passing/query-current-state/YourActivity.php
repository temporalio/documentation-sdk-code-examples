<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\QueryCurrentState;

class YourActivity implements YourActivityInterface
{
    public function doSomething(string $input): string
    {
        return "did something with {$input}";
    }
}
