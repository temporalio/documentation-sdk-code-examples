<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\WorkflowInit;

use Temporal\Activity\ActivityInterface;
use Temporal\Activity\ActivityMethod;

#[ActivityInterface(prefix: 'activity.')]
class TitleActivity
{
    #[ActivityMethod]
    public function checkTitleValidity(string $nameWithTitle): bool
    {
        return \str_starts_with($nameWithTitle, 'Sir ');
    }
}
