<?php // @@@SNIPSTART php-activity-basics-define ?>
<?php
declare(strict_types=1);

namespace App\Activities\Basics\Define;

use Temporal\Activity\ActivityInterface;
use Temporal\Activity\ActivityMethod;

#[ActivityInterface]
// Defining an interface for the activities.
interface FileProcessingActivities
{
    public function upload(string $bucketName, string $localName, string $targetName): void;

    #[ActivityMethod("transcode_file")]
    public function download(string $bucketName, string $remoteName): void;

    public function processFile(): string;

    public function deleteLocalFile(string $fileName): void;
}
// @@@SNIPEND
