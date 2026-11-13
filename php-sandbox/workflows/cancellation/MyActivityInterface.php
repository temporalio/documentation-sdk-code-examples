<?php
declare(strict_types=1);

namespace App\Workflows\Cancellation;

use Temporal\Activity\ActivityInterface;
use Temporal\Activity\ActivityMethod;

#[ActivityInterface]
interface MyActivityInterface
{
    #[ActivityMethod]
    public function processItems(int $count): string;
}
