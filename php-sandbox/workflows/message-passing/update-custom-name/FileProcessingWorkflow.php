<?php // @@@SNIPSTART php-message-passing-update-custom-name ?>
<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\UpdateCustomName;

use Temporal\Workflow\UpdateMethod;
use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
interface FileProcessingWorkflow {
    #[WorkflowMethod]
    public function processFiles(FileList $files);

    #[UpdateMethod(name: 'pause')]
    public function pauseProcessing(): void;
}
// @@@SNIPEND
