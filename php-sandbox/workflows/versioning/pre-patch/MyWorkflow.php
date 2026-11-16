<?php // @@@SNIPSTART php-versioning-pre-patch ?>
<?php
declare(strict_types=1);

namespace App\Workflows\Versioning\PrePatch;

use App\Workflows\Versioning\YourActivityInterface;
use Temporal\Activity\ActivityOptions;
use Temporal\Workflow;
use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
class MyWorkflow
{
    private $activity;

    public function __construct()
    {
        $this->activity = Workflow::newActivityStub(
            YourActivityInterface::class,
            ActivityOptions::new()->withStartToCloseTimeout(60)
        );
    }

    #[WorkflowMethod]
    public function runAsync()
    {
        $result = yield $this->activity->prePatchActivity();
    }
}
// @@@SNIPEND
