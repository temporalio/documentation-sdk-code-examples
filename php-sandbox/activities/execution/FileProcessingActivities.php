<?php
declare(strict_types=1);

namespace App\Activities\Execution;

use Temporal\Activity\ActivityInterface;

#[ActivityInterface]
interface FileProcessingActivities
{
    public function upload(string $bucketName, string $localName, string $targetName): void;

    public function download(string $bucketName, string $remoteName, string $localName): void;

    public function processFile(string $localName): string;

    public function deleteLocalFile(string $fileName): void;
}
