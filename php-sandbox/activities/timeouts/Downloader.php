<?php
declare(strict_types=1);

namespace App\Activities\Timeouts;

// Stands in for a client that downloads a file in chunks and reports progress.
class Downloader
{
    public function __construct(
        private readonly int $chunks = 5,
        private readonly int $secondsPerChunk = 1,
    ) {}

    /** @param callable(int $progress): void $onProgress Called with the percentage done after each chunk */
    public function downloadWithProgress(string $bucketName, string $remoteName, string $localName, callable $onProgress): void
    {
        for ($chunk = 1; $chunk <= $this->chunks; $chunk++) {
            \sleep($this->secondsPerChunk);
            $onProgress(\intdiv($chunk * 100, $this->chunks + 1));
        }
    }
}
