<?php // @@@SNIPSTART php-workflow-basics-workflow-init ?>
<?php
declare(strict_types=1);

namespace App\Workflows\Basics\WorkflowInit;

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
}
// @@@SNIPEND
