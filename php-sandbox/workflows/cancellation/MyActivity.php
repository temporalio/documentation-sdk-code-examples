<?php // @@@SNIPSTART php-cancellation-activity ?>
<?php
declare(strict_types=1);

namespace App\Workflows\Cancellation;

use Temporal\Activity;
use Temporal\Exception\Client\ActivityCanceledException;

class MyActivity implements MyActivityInterface
{
    public function processItems(int $count): string
    {
        for ($i = 0; $i < $count; $i++) {
            // Simulate one unit of work.
            \sleep(1);

            try {
                // Heartbeat after each item. This is where the Activity learns that
                // it has been canceled.
                Activity::heartbeat($i);
            } catch (ActivityCanceledException $e) {
                // Clean up, then rethrow so that the Activity appears canceled.
                \error_log("processItems canceled after {$i} items, cleaning up");
                throw $e;
            }
        }

        return "Processed {$count} items";
    }
}
// @@@SNIPEND
