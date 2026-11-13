<?php // @@@SNIPSTART php-message-passing-handle-signal ?>
<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\HandleSignal;

use Temporal\Workflow;

#[Workflow\WorkflowInterface]
class YourWorkflow
{
    private bool $value = false;

    #[Workflow\WorkflowMethod]
    public function run()
    {
        yield Workflow::await(fn()=> $this->value);
        return 'OK';
    }

    #[Workflow\SignalMethod]
    public function setValue(bool $value): void
    {
        $this->value = $value;
    }
}
// @@@SNIPEND
