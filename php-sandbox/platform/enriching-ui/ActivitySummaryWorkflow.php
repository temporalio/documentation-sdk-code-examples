<?php // @@@SNIPSTART php-enriching-ui-activity-summary ?>
<?php
declare(strict_types=1);

namespace App\Platform\EnrichingUi\ActivitySummary;

use App\Platform\EnrichingUi\TimerSummary\YourWorkflow;
use Temporal\Activity\ActivityOptions;
use Temporal\Workflow;

class YourWorkflowImpl implements YourWorkflow
{
    /** @var YourActivitiesInterface */
    private $activities;
    
    public function __construct()
    {
        $this->activities = Workflow::newActivityStub(
            YourActivitiesInterface::class,
            ActivityOptions::new()
                ->withStartToCloseTimeout('10 seconds')
                ->withSummary('Processing user data')
        );
    }
    
    public function yourWorkflowMethod(string $input): \Generator
    {
        // Execute the activity with the summary
        $result = yield $this->activities->yourActivity($input);
        
        return $result;
    }
}
// @@@SNIPEND
