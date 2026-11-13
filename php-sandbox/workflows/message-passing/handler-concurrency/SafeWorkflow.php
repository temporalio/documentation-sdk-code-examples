<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\HandlerConcurrency;

use Temporal\Activity\ActivityOptions;
use Temporal\Workflow;

#[Workflow\WorkflowInterface]
class SafeWorkflow
{
    public mixed $x = null;
    public mixed $y = null;

    #[Workflow\WorkflowMethod]
    public function run()
    {
        yield Workflow::await(fn() => $this->x === 'done');
    }

    #[Workflow\QueryMethod]
    public function getXY(): array
    {
        return [$this->x, $this->y];
    }

    // @@@SNIPSTART php-message-passing-safe-handler
    private Workflow\Mutex $mutex;

    public function __construct()
    {
        $this->mutex = new Workflow\Mutex();
    }

    #[Workflow\SignalMethod]
    public function safeAsyncHandler()
    {
        $data = yield Workflow::executeActivity(
            type: 'fetch_data',
            args: ['url' => 'http://example.com'],
            options: ActivityOptions::new()->withStartToCloseTimeout('10 seconds'),
        );
        yield Workflow::runLocked($this->mutex, function () use ($data) {
            $this->x = $data->x;
            # ✅ OK: the scheduler may switch now to a different handler execution, or to the main workflow
            # method, but no other execution of this handler can run until this execution finishes.
            yield Workflow::timer(1);  # or await anything else
            $this->y = $data->y;
        });
    }
    // @@@SNIPEND
}
