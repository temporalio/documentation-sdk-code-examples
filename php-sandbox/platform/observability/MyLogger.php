<?php
declare(strict_types=1);

namespace App\Platform\Observability;

use Psr\Log\AbstractLogger;

// A PSR-3 logger that writes "[MyLogger] LEVEL message {context}" lines to stderr.
class MyLogger extends AbstractLogger
{
    public function log($level, \Stringable|string $message, array $context = []): void
    {
        \error_log(\sprintf('[MyLogger] %s %s %s', \strtoupper((string) $level), $message, \json_encode($context)));
    }
}
