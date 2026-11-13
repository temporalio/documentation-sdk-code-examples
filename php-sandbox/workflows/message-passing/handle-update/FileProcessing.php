<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\HandleUpdate;

use Temporal\Workflow;

class FileProcessing implements FileProcessingWorkflow
{
    private bool $paused = false;

    public function processFile(File $file)
    {
        yield Workflow::await(fn() => $this->paused);
        return new ProcessResult($file->name, $this->paused);
    }

    public function pauseProcessing(): void
    {
        $this->paused = true;
    }
}
