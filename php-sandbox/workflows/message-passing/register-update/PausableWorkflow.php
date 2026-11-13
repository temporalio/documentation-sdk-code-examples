<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\RegisterUpdate;

use Temporal\Workflow;

#[Workflow\WorkflowInterface]
class PausableWorkflow
{
    private bool $paused = false;

    #[Workflow\WorkflowMethod]
    public function run()
    {
        // @@@SNIPSTART php-message-passing-register-update
        Workflow::registerUpdate(
            name: 'pause',
            handler: fn() => $this->paused = true,
            validator: fn() => $this->paused === false or throw new \Exception('Workflow is already paused'),
        );
        // @@@SNIPEND

        yield Workflow::await(fn() => $this->paused);
        return 'paused';
    }
}
