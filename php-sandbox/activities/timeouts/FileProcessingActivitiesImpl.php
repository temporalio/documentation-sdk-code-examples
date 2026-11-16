<?php // @@@SNIPSTART php-activity-timeouts-heartbeat ?>
<?php
declare(strict_types=1);

namespace App\Activities\Timeouts;

use Temporal\Activity;

class FileProcessingActivitiesImpl implements FileProcessingActivities
{
    public function __construct(
        private readonly Downloader $downloader,
    ) {}

    public function download(
        string $bucketName,
        string $remoteName,
        string $localName
    ): void
    {
        $this->downloader->downloadWithProgress(
            $bucketName,
            $remoteName,
            $localName,
            // on progress
            function ($progress) {
                Activity::heartbeat($progress);
            }
        );

        Activity::heartbeat(100); // download complete
    }
}
// @@@SNIPEND
