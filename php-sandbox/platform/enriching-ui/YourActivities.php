<?php
declare(strict_types=1);

namespace App\Platform\EnrichingUi\ActivitySummary;

class YourActivities implements YourActivitiesInterface
{
    public function yourActivity(string $input): string
    {
        return "processed {$input}";
    }
}
