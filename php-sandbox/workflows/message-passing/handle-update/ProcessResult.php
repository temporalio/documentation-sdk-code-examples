<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\HandleUpdate;

final class ProcessResult
{
    public function __construct(
        public string $fileName = '',
        public bool $paused = false,
    ) {}
}
