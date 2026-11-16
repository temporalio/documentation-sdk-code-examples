<?php
declare(strict_types=1);

namespace App\Activities\Basics\CustomName;

class FileProcessingActivitiesImpl implements FileProcessingActivities
{
    public function upload(string $bucketName, string $localName, string $targetName) {}

    public function download(string $bucketName, string $remoteName) {}

    public function processFile(): string
    {
        return 'processed';
    }

    public function deleteLocalFile(string $fileName) {}
}
