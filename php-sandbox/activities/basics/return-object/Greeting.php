<?php
declare(strict_types=1);

namespace App\Activities\Basics\ReturnObject;

final class Greeting
{
    public function __construct(
        public string $greeting,
        public string $name,
    ) {}
}
