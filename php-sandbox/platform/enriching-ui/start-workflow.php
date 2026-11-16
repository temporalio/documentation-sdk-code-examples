<?php // @@@SNIPSTART php-enriching-ui-start-workflow ?>
<?php
use App\Platform\EnrichingUi\TimerSummary\YourWorkflow;
use Temporal\Client\WorkflowClient;
use Temporal\Client\WorkflowOptions;

// Create workflow client
$workflowClient = WorkflowClient::create($serviceClient);

// Start a workflow with static summary and details
$workflow = $workflowClient->newWorkflowStub(
    YourWorkflow::class,
    WorkflowOptions::new()
        ->withWorkflowId('your-workflow-id')
        ->withTaskQueue('your-task-queue')
        ->withStaticSummary('Order processing for customer #12345')
        ->withStaticDetails('Processing premium order with expedited shipping')
);

$result = $workflow->yourWorkflowMethod('workflow input');
// @@@SNIPEND
