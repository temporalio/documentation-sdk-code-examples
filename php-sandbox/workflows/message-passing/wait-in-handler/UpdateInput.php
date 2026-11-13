<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\WaitInHandler;

final class UpdateInput
{
    public function __construct(
        public string $value = '',
    ) {}
}
