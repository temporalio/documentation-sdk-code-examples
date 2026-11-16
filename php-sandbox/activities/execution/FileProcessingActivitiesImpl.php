<?php
declare(strict_types=1);

namespace App\Activities\Execution;

use Aws\S3\S3Client;

// @@@SNIPSTART php-activity-execution-implementation
// An implementation of an Activity interface.
class FileProcessingActivitiesImpl implements FileProcessingActivities {

    private S3Client $s3Client;

    private string $localDirectory;

    public function __construct(S3Client $s3Client, string $localDirectory) {
        $this->s3Client = $s3Client;
        $this->localDirectory = $localDirectory;
    }

    // Uploading a file to S3.
    public function upload(string $bucketName, string $localName, string $targetName): void
    {
        $this->s3Client->putObject([
            'Bucket' => $bucketName,
            'Key' => $targetName,
            'Body' => fopen($this->localDirectory . $localName, 'rb'),
        ]);
    }

    // Downloading a file from S3.
    public function download(
        string $bucketName,
        string $remoteName,
        string $localName
    ): void
    {
        $this->s3Client->getObject([
            'Bucket' => $bucketName,
            'Key' => $remoteName,
            'SaveAs' => $this->localDirectory . $localName,
        ]);
    }

    // A function that takes a local file name as an argument and returns a string.
    public function processFile(string $localName): string
    {
        return $this->compressFile($this->localDirectory . $localName);
    }

    public function deleteLocalFile(string $fileName): void
    {
        unlink($this->localDirectory . $fileName);
    }

    // Compresses a file with gzip and returns the path of the compressed file.
    private function compressFile(string $path): string
    {
        file_put_contents($path . '.gz', gzencode(file_get_contents($path)));
        return $path . '.gz';
    }
}
// @@@SNIPEND
