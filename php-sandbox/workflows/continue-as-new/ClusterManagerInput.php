<?php // @@@SNIPSTART php-continue-as-new-input ?>
<?php
declare(strict_types=1);

namespace App\Workflows\ContinueAsNew;

final class ClusterManagerInput
{
    public function __construct(
        public ?ClusterManagerState $state = null,
        public bool $testContinueAsNew = false,
    ) {}
}
// @@@SNIPEND
