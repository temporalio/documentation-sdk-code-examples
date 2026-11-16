<?php // @@@SNIPSTART php-enriching-ui-timer-summary ?>
<?php
declare(strict_types=1);

namespace App\Platform\EnrichingUi\TimerSummary;

use Temporal\Workflow;
use Temporal\Workflow\ReturnType;
use Temporal\Workflow\TimerOptions;
use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
interface YourWorkflow
{
    #[WorkflowMethod]
    #[ReturnType('string')]
    public function yourWorkflowMethod(string $input);
}

class YourWorkflowImpl implements YourWorkflow
{
    public function yourWorkflowMethod(string $input): \Generator
    {
        // Create a timer with a summary
        yield Workflow::timer(
            300, // 5 minutes in seconds
            TimerOptions::new()->withSummary('Waiting for payment confirmation')
        );
        
        return 'Timer completed';
    }
}
// @@@SNIPEND
