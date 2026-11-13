<?php // @@@SNIPSTART php-workflow-basics-default-name ?>
<?php
declare(strict_types=1);

namespace App\Workflows\Basics\DefaultName;

use App\Workflows\Basics\Argument;
use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
interface YourWorkflowDefinitionInterface
{
    #[WorkflowMethod]
    public function processFile(Argument $args);
}
// @@@SNIPEND
