<?php
declare(strict_types=1);

namespace App\BestPractices\TestingSuite;

class SimpleActivity implements SimpleActivityInterface
{
    public function doSomething(string $input): string
    {
        return \strtoupper($input);
    }
}
