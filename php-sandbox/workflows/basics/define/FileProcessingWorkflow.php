<?php // @@@SNIPSTART php-workflow-basics-define ?>
<?php
declare(strict_types=1);

namespace App\Workflows\Basics\Define;

use App\Workflows\Basics\Argument;
use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
interface FileProcessingWorkflow
{
    #[WorkflowMethod]
    public function processFile(Argument $args);
}
// @@@SNIPEND
