<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\HandlerConcurrency;

use Temporal\Activity\ActivityInterface;
use Temporal\Activity\ActivityMethod;

#[ActivityInterface]
class FetchDataActivity
{
    private static int $calls = 0;

    #[ActivityMethod(name: 'fetch_data')]
    public function fetchData(string $url): array
    {
        $n = ++self::$calls;
        return ['x' => "x{$n}", 'y' => "y{$n}"];
    }
}
