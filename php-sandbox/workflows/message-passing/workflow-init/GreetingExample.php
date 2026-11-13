<?php // @@@SNIPSTART php-message-passing-workflow-init ?>
<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\WorkflowInit;

use Temporal\Activity\ActivityOptions;
use Temporal\Workflow;

#[Workflow\WorkflowInterface]
class GreetingExample
{
    private readonly string $nameWithTitle;
    private bool $titleHasBeenChecked;

    // Note the attribute is on a public constructor
    #[Workflow\WorkflowInit]
    public function __construct(string $input)
    {
        $this->nameWithTitle = 'Sir ' . $input;
        $this->titleHasBeenChecked = false;
    }

    #[Workflow\WorkflowMethod]
    public function getGreeting(string $input)
    {
        yield Workflow::await(fn() => $this->titleHasBeenChecked);
        return "Hello " . $this->nameWithTitle;
    }

    #[Workflow\UpdateMethod]
    public function checkTitleValidity()
    {
        // 👉 The handler is now guaranteed to see the workflow input
        // after it has been processed by the constructor.
        $isValid = yield Workflow::executeActivity(
            'activity.checkTitleValidity',
            [$this->nameWithTitle],
            ActivityOptions::new()->withStartToCloseTimeout('10 seconds'),
        );
        $this->titleHasBeenChecked = true;
        return $isValid;
    }
}
// @@@SNIPEND
