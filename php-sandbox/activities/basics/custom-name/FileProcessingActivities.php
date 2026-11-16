<?php // @@@SNIPSTART php-activity-basics-custom-name ?>
<?php
declare(strict_types=1);

namespace App\Activities\Basics\CustomName;

use Temporal\Activity\ActivityInterface;
use Temporal\Activity\ActivityMethod;

#[ActivityInterface("file_activities.")]
interface FileProcessingActivities
{
    public function upload(string $bucketName, string $localName, string $targetName);

    #[ActivityMethod("transcode_file")]
    public function download(string $bucketName, string $remoteName);

    public function processFile(): string;

    public function deleteLocalFile(string $fileName);
}
// @@@SNIPEND
