<?php
declare(strict_types=1);

namespace App\Client\TemporalClient;

use App\Client\TemporalClient\Updates\AccountTransferWorkflowInterface;
use Ramsey\Uuid\UuidInterface;
use Temporal\Client\GRPC\ServiceClientInterface;
use Temporal\Client\WorkflowClientInterface;
use Temporal\Client\WorkflowOptions;

// Client code from the Temporal Client page. Each method holds one example;
// its parameters are the variables the example uses.
final class ClientExamples
{
    public function typedStub(
        WorkflowClientInterface $workflowClient,
        UuidInterface $transactionId,
        UuidInterface $from1, UuidInterface $to1, int $amount1,
        UuidInterface $from2, UuidInterface $to2, int $amount2,
    ): void {
        // @@@SNIPSTART php-client-typed-stub
        $stub = $workflowClient->newWorkflowStub(AccountTransferWorkflowInterface::class);

        $workflowClient->start($stub, $transactionId);
        $stub->move($from1, $to1, $amount1);
        $stub->move($from2, $to2, $amount2);
        $stub->commit();
        // @@@SNIPEND
    }

    public function untypedStub(
        WorkflowClientInterface $workflowClient,
        UuidInterface $transactionId,
        UuidInterface $from1, UuidInterface $to1, int $amount1,
        UuidInterface $from2, UuidInterface $to2, int $amount2,
    ): void {
        // @@@SNIPSTART php-client-untyped-stub
        $stub = $workflowClient->newUntypedWorkflowStub('account.transfer');

        $workflowClient->start($stub, $transactionId);
        $stub->update('pay', $from1, $to1, $amount1);
        $stub->update('pay', $from2, $to2, $amount2);
        $stub->update('finish');
        // @@@SNIPEND
    }

    public function taskQueue(WorkflowClientInterface $workflowClient): object
    {
        // @@@SNIPSTART php-client-task-queue
        $stub = $workflowClient->newWorkflowStub(
            YourWorkflowInterface::class,
            WorkflowOptions::new()
                ->withTaskQueue("Workflow-Task-Queue-1"),
        );
        // @@@SNIPEND

        return $stub;
    }

    public function workflowId(WorkflowClientInterface $workflowClient): object
    {
        // @@@SNIPSTART php-client-workflow-id
        $stub = $workflowClient->newWorkflowStub(
            YourWorkflowInterface::class,
            WorkflowOptions::new()
                ->withWorkflowId("Workflow-Id"),
        );
        // @@@SNIPEND

        return $stub;
    }

    public function getResult(WorkflowClientInterface $workflowClient): void
    {
        // @@@SNIPSTART php-client-get-result
        $stub = $workflowClient->newWorkflowStub(YourWorkflowInterface::class);
        $run = $workflowClient->start($stub, 'fromID', 'toID', 'refID', 1000);

        var_dump($run->getResult());
        // @@@SNIPEND
    }

    public function getUntypedResult(WorkflowClientInterface $workflowClient): void
    {
        // @@@SNIPSTART php-client-get-untyped-result
        $stub = $workflowClient->newUntypedWorkflowStub('account.transfer');
        $workflowClient->start($stub, 'fromID', 'toID', 'refID', 1000);

        var_dump($stub->getResult(timeout: 5));
        // @@@SNIPEND
    }

    public function connect(ServiceClientInterface $serviceClient): void
    {
        // @@@SNIPSTART php-client-connection
        // ...
        $serviceClient->getConnection()->connect(timeout: 10);

        // or
        $serviceClient->getServerCapabilities();
        // @@@SNIPEND
    }
}
