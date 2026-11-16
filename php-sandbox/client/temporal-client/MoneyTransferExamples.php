<?php
declare(strict_types=1);

namespace App\Client\TemporalClient;

use App\Client\TemporalClient\MoneyTransfer\AccountTransferWorkflowInterface;
use Ramsey\Uuid\UuidInterface;
use Temporal\Client\WorkflowClientInterface;

// Client code from the "Start a Workflow synchronously" and "asynchronously"
// sections of the Temporal Client page, which use the MoneyTransfer interface.
final class MoneyTransferExamples
{
    public function startSync(WorkflowClientInterface $workflowClient): UuidInterface
    {
        // @@@SNIPSTART php-client-start-sync
        $accountTransfer = $workflowClient->newWorkflowStub(
            AccountTransferWorkflowInterface::class,
        );

        $result = $accountTransfer->transfer('fromID', 'toID', 'refID', 1000);
        // @@@SNIPEND

        return $result;
    }

    public function startAsync(WorkflowClientInterface $workflowClient): object
    {
        // @@@SNIPSTART php-client-start-async
        $accountTransfer = $workflowClient->newWorkflowStub(
            AccountTransferWorkflowInterface::class,
        );

        $run = $workflowClient->start($accountTransfer, 'fromID', 'toID', 'refID', 1000);
        // @@@SNIPEND

        return $run;
    }

    public function runDetails(WorkflowClientInterface $workflowClient, object $accountTransfer): void
    {
        // @@@SNIPSTART php-client-workflow-run
        $run = $workflowClient->start($accountTransfer, 'fromID', 'toID', 'refID', 1000);

        // Get the Workflow ID
        var_dump($run->getExecution()->getID());

        // Describe the Workflow Execution
        var_dump($run->describe());

        // Wait for the Workflow to complete and get the result with 10-second timeout
        var_dump($run->getResult(timeout: 10));
        // @@@SNIPEND
    }
}
