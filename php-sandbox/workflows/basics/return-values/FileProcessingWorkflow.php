<?php // @@@SNIPSTART php-workflow-basics-return-type ?>
<?php
declare(strict_types=1);

namespace App\Workflows\Basics\ReturnValues;

use App\Workflows\Basics\Argument;
use Temporal\Workflow\ReturnType;
use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
interface FileProcessingWorkflow
{
    #[WorkflowMethod]
    #[ReturnType("string")]
    public function processFile(Argument $args);
}
// @@@SNIPEND
