<?php
declare(strict_types=1);

namespace App\Workflows\Versioning\Patched;

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

    // @@@SNIPSTART php-versioning-patched
    #[WorkflowMethod]
    public function runAsync()
    {
        $version = yield Workflow::getVersion('Step 1', Workflow::DEFAULT_VERSION, 1);

        $result = $version === Workflow::DEFAULT_VERSION
            ? yield $this->activity->prePatchActivity()
            : yield $this->activity->postPatchActivity();
    }
    // @@@SNIPEND
}
