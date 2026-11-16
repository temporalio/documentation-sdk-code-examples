<?php
declare(strict_types=1);

namespace App\Workflows\Versioning\MinSupported1;

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

    // @@@SNIPSTART php-versioning-min-supported-1
    #[WorkflowMethod]
    public function runAsync()
    {
        $version = yield Workflow::getVersion('Step 1', minSupported: 1, maxSupported: 2);

        $result = match($version) {
            1 => yield $this->activity->postPatchActivity(),
            2 => yield $this->activity->anotherPatchActivity(),
        };
    }
    // @@@SNIPEND
}
