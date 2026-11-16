<?php
declare(strict_types=1);

namespace App\Platform\EnrichingUi\ActivitySummary;

use Temporal\Activity\ActivityInterface;
use Temporal\Activity\ActivityMethod;

#[ActivityInterface]
interface YourActivitiesInterface
{
    #[ActivityMethod]
    public function yourActivity(string $input): string;
}
