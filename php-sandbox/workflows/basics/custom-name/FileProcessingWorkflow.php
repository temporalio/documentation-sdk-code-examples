<?php
declare(strict_types=1);

namespace App\Workflows\Basics\CustomName;

use App\Workflows\Basics\Argument;
use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
interface FileProcessingWorkflow
{
    // @@@SNIPSTART php-workflow-basics-custom-name
    #[WorkflowMethod(name: "FileProcessing")]
    // @@@SNIPEND
    public function processFile(Argument $args);
}
