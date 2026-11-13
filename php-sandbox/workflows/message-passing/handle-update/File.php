<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\HandleUpdate;

final class File
{
    public function __construct(
        public string $name = '',
    ) {}
}
