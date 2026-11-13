<?php // @@@SNIPSTART php-message-passing-query-current-state ?>
<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\QueryCurrentState;

use DateInterval;
use Temporal\Activity\ActivityOptions;
use Temporal\Workflow;

#[Workflow\WorkflowInterface]
class YourWorkflow
{
    private string $currentState;

    #[Workflow\QueryMethod('current_state')]
    public function getCurrentState(): string
    {
        return $this->currentState;
    }

    #[Workflow\WorkflowMethod]
    public function run()
    {
        // Your normal Workflow code begins here, and you update the currentState
        // as the code makes progress.
        $this->currentState = 'waiting timer';
        try{
            yield Workflow::timer(DateInterval::createFromDateString('1 hour'));
        } catch (\Throwable $e) {
            $this->currentState = 'timer failed';
            throw $e;
        }

        $yourActivity = Workflow::newActivityStub(
            YourActivityInterface::class,
            ActivityOptions::new()->withStartToCloseTimeout(60)
        );

        $this->currentState = 'waiting activity';
        try{
            yield $yourActivity->doSomething('some input');
        } catch (\Throwable $e) {
            $this->currentState = 'activity failed';
            throw $e;
        }

        $this->currentState = 'done';

        return null;
    }
}
// @@@SNIPEND
