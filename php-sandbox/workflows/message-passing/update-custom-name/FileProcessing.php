<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\UpdateCustomName;

use Temporal\Workflow;

class FileProcessing implements FileProcessingWorkflow
{
    private bool $paused = false;

    public function processFiles(FileList $files)
    {
        yield Workflow::await(fn() => $this->paused);
        return \count($files->names);
    }

    public function pauseProcessing(): void
    {
        $this->paused = true;
    }
}
