<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\QueryCurrentState;

use Temporal\Activity\ActivityInterface;
use Temporal\Activity\ActivityMethod;

#[ActivityInterface]
interface YourActivityInterface
{
    #[ActivityMethod]
    public function doSomething(string $input): string;
}
