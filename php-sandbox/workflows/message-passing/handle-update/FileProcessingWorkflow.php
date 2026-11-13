<?php // @@@SNIPSTART php-message-passing-handle-update ?>
<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\HandleUpdate;

use Temporal\Workflow\ReturnType;
use Temporal\Workflow\UpdateMethod;
use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
interface FileProcessingWorkflow {
    #[WorkflowMethod]
    #[ReturnType(ProcessResult::class)]
    public function processFile(File $file);

    #[UpdateMethod]
    public function pauseProcessing(): void;
}
// @@@SNIPEND
