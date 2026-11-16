<?php // @@@SNIPSTART php-observability-workflow-logger ?>
<?php
declare(strict_types=1);

namespace App\Platform\Observability;

use Temporal\Workflow;

#[Workflow\WorkflowInterface]
class MyWorkflow
{
    #[Workflow\WorkflowMethod]
    public function execute(string $param)
    {
        Workflow::getLogger()->info('Workflow started', ['parameter' => $param]);

        // Your workflow implementation

        Workflow::getLogger()->info('Workflow completed');
        return 'Done';
    }
}
// @@@SNIPEND
