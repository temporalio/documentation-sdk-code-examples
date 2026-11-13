<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\HandlerConcurrency;

use Temporal\Activity\ActivityOptions;
use Temporal\Workflow;

#[Workflow\WorkflowInterface]
class BadWorkflow
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

    // @@@SNIPSTART php-message-passing-bad-handler
    #[Workflow\SignalMethod]
    public function badAsyncHandler()
    {
        $data = yield Workflow::executeActivity(
            type: 'fetch_data',
            args: ['url' => 'http://example.com'],
            options: ActivityOptions::new()->withStartToCloseTimeout('10 seconds'),
        );
        $this->x = $data->x;
        # 🐛🐛 Bug!! If multiple instances of this handler are executing concurrently, then
        # there may be times when the Workflow has $this->x from one Activity execution and $this->y from another.
        yield Workflow::timer(1);  # or await anything else
        $this->y = $data->y;
    }
    // @@@SNIPEND
}
