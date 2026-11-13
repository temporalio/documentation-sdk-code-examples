<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\Client;

use App\Workflows\MessagePassing\HandleSignal\YourWorkflow;
use App\Workflows\MessagePassing\QueryCurrentState\YourWorkflow as YourQueryWorkflow;
use App\Workflows\MessagePassing\ValidateUpdate\GreetingWorkflow;
use Ramsey\Uuid\UuidInterface;
use Temporal\Client\Update\LifecycleStage;
use Temporal\Client\Update\UpdateOptions;
use Temporal\Client\WorkflowClientInterface;
use Temporal\Client\WorkflowOptions;
use Temporal\Common\WorkflowIdConflictPolicy;

// Client code from the Workflow message passing page. Each method holds one
// example; its parameters are the variables the example uses.
final class MessagePassingClient
{
    public function sendSignal(WorkflowClientInterface $workflowClient): void
    {
        // @@@SNIPSTART php-message-passing-send-signal
        $workflow = $workflowClient->newWorkflowStub(YourWorkflow::class);

        $run = $workflowClient->start($workflow);

        // do something

        $workflow->setValue(true);

        assert($run->getResult() === 'OK');
        // @@@SNIPEND
    }

    public function sendSignalToRunningWorkflow(WorkflowClientInterface $workflowClient): void
    {
        // @@@SNIPSTART php-message-passing-send-signal-running
        $workflow = $workflowClient->newRunningWorkflowStub(YourWorkflow::class, 'workflowID');
        $workflow->setValue(true);
        // @@@SNIPEND
    }

    public function signalWithStart(WorkflowClientInterface $workflowClient): object
    {
        // @@@SNIPSTART php-message-passing-signal-with-start
        $workflow = $workflowClient->newWorkflowStub(YourWorkflow::class);

        $run = $workflowClient->signalWithStart(
            $workflow,
            'setValue',
            [true], // signal arguments
            [] // start arguments
        );
        // @@@SNIPEND

        return $run;
    }

    public function query(WorkflowClientInterface $workflowClient): void
    {
        // @@@SNIPSTART php-message-passing-query-from-client
        $workflow = $workflowClient->newWorkflowStub(
            YourQueryWorkflow::class,
            WorkflowOptions::new()
        );

        $workflowClient->start($workflow);

        var_dump($workflow->getCurrentState());
        sleep(60);
        var_dump($workflow->getCurrentState());
        // @@@SNIPEND
    }

    public function sendUpdate(WorkflowClientInterface $workflowClient, WorkflowOptions $workflowOptions): int
    {
        // @@@SNIPSTART php-message-passing-send-update
        // Create a typed Workflow stub for GreetingsWorkflow
        $workflow = $workflowClient->newWorkflowStub(GreetingWorkflow::class, $workflowOptions);

        // Start the Workflow
        $run = $workflowClient->start($workflow);

        // Send an update to the Workflow. addGreeting returns
        // the number of greetings our workflow has received.
        $count = $workflow->addGreeting("World");
        // @@@SNIPEND

        return $count;
    }

    public function startUpdate(WorkflowClientInterface $client, WorkflowOptions $workflowOptions): array
    {
        // @@@SNIPSTART php-message-passing-start-update
        // Create an untyped Workflow stub for GreetingsWorkflow
        $stub = $client->newUntypedWorkflowStub('GreetingWorkflow', $workflowOptions);

        // Start the Workflow
        $run = $client->start($stub);

        // Send an Update to the Workflow. UpdateHandle returns
        $handle = $stub->startUpdate('addGreeting', 'World');

        // Use the UpdateHandle to get the Update result with timeout 2.5 seconds
        $result = $handle->getResult(timeout: 2.5);

        // You can get more control using UpdateOptions
        $resultUuid = $stub->startUpdate(
            UpdateOptions::new('storeGreetings', LifecycleStage::StageCompleted)
                ->withResultType(UuidInterface::class)
         )->getResult();
        // @@@SNIPEND

        return [$result, $resultUuid];
    }

    public function updateWithStart(
        WorkflowClientInterface $workflowClient,
        string $cartId,
        string $itemId,
        int $quantity,
    ): int {
        // @@@SNIPSTART php-message-passing-update-with-start
        $stub = $workflowClient->newUntypedWorkflowStub(
            'ShoppingCartWorkflow',
            WorkflowOptions::new()
                ->withTaskQueue('service-queue')
                ->withWorkflowId($cartId)
                ->withWorkflowIdConflictPolicy(WorkflowIdConflictPolicy::UseExisting),
        );
        $handle = $workflowClient->updateWithStart(
            workflow: $stub,
            update: 'addItem',
            updateArgs: [$itemId, $quantity],
        );

        $price = $handle->getResult();
        // @@@SNIPEND

        return $price;
    }

    public function updateWithStartAndWait(
        WorkflowClientInterface $workflowClient,
        string $cartId,
        string $itemId,
        int $quantity,
    ): int {
        $stub = $workflowClient->newUntypedWorkflowStub(
            'ShoppingCartWorkflow',
            WorkflowOptions::new()
                ->withTaskQueue('service-queue')
                ->withWorkflowId($cartId)
                ->withWorkflowIdConflictPolicy(WorkflowIdConflictPolicy::UseExisting),
        );

        // @@@SNIPSTART php-message-passing-update-with-start-completed
        $handle = $workflowClient->updateWithStart(
            workflow: $stub,
            update: UpdateOptions::new('addItem', LifecycleStage::StageCompleted),
            updateArgs: [$itemId, $quantity],
        );

        assert($handle->hasResult() === true);
        $price = $handle->getResult();
        // @@@SNIPEND

        return $price;
    }
}
