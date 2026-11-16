<?php
declare(strict_types=1);

namespace App\Workflows\Versioning\AnotherPatch;

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

    // @@@SNIPSTART php-versioning-another-patch
    #[WorkflowMethod]
    public function runAsync()
    {
        $version = yield Workflow::getVersion('Step 1', Workflow::DEFAULT_VERSION, maxSupported: 2);

        $result = match($version) {
            Workflow::DEFAULT_VERSION => yield $this->activity->prePatchActivity(),
            1 => yield $this->activity->postPatchActivity(),
            2 => yield $this->activity->anotherPatchActivity(),
        };
    }
    // @@@SNIPEND
}
