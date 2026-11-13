<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\Define;

use App\Workflows\Basics\Argument;
use Temporal\Workflow;

class FileProcessing implements FileProcessingWorkflow
{
    private array $history = [];
    private string $status = 'processing';

    public function processFile(Argument $args)
    {
        $this->history[] = "started {$args->sourceUrl}";
        yield Workflow::await(fn() => $this->status === 'abandoned');
        return $this->history;
    }

    public function getHistory(): array
    {
        return $this->history;
    }

    public function getStatus(): string
    {
        return $this->status;
    }

    public function retryNow(): void
    {
        $this->history[] = 'retried';
    }

    public function abandon(): void
    {
        $this->history[] = 'abandoned';
        $this->status = 'abandoned';
    }
}
