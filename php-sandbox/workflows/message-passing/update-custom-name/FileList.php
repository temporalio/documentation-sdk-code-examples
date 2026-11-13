<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\UpdateCustomName;

final class FileList
{
    public function __construct(
        /** @var list<string> */
        public array $names = [],
    ) {}
}
