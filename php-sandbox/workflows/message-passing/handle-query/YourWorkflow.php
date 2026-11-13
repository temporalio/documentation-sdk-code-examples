<?php // @@@SNIPSTART php-message-passing-handle-query ?>
<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\HandleQuery;

use Temporal\Workflow;

#[Workflow\WorkflowInterface]
class YourWorkflow
{
    #[Workflow\QueryMethod]
    public function getValue()
    {
        return 42;
    }

    #[Workflow\WorkflowMethod]
    public function run()
    {
        // workflow code
    }
}
// @@@SNIPEND
