<?php
declare(strict_types=1);

namespace App\Activities\Timeouts;

use Temporal\Activity\ActivityInterface;
use Temporal\Activity\ActivityMethod;

#[ActivityInterface(prefix: 'Timeouts.')]
interface FileProcessingActivities
{
    #[ActivityMethod]
    public function download(string $bucketName, string $remoteName, string $localName): void;
}
