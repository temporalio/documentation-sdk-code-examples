<?php
declare(strict_types=1);

namespace App\Activities\Basics\Define;

class FileProcessingActivitiesImpl implements FileProcessingActivities
{
    public function upload(string $bucketName, string $localName, string $targetName): void {}

    public function download(string $bucketName, string $remoteName): void {}

    public function processFile(): string
    {
        return 'processed';
    }

    public function deleteLocalFile(string $fileName): void {}
}
