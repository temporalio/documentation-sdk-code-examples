<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\WaitInHandler;

use Temporal\Workflow;
use Temporal\Workflow\SignalMethod;
use Temporal\Workflow\UpdateMethod;
use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
class MyWorkflow
{
    private bool $ready = false;
    private ?string $value = null;

    #[WorkflowMethod]
    public function run()
    {
        yield Workflow::await(fn() => $this->value !== null);
        return $this->value;
    }

    #[SignalMethod]
    public function markReady(): void
    {
        $this->ready = true;
    }

    // @@@SNIPSTART php-message-passing-wait-in-handler
    #[UpdateMethod]
    public function myUpdate(UpdateInput $input)
    {
        yield Workflow::await(
            fn() => $this->readyForUpdateToExecute($input),
        );

        $this->value = $input->value;
        return $this->value;
    }
    // @@@SNIPEND

    private function readyForUpdateToExecute(UpdateInput $input): bool
    {
        return $this->ready;
    }
}
