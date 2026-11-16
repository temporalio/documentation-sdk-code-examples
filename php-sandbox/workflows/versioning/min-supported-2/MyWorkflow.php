<?php
declare(strict_types=1);

namespace App\Workflows\Versioning\MinSupported2;

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

    // @@@SNIPSTART php-versioning-min-supported-2
    #[WorkflowMethod]
    public function runAsync()
    {
        $version = yield Workflow::getVersion('Step 1', minSupported: 2, maxSupported: 2);

        $result = yield $this->activity->anotherPatchActivity();
    }
    // @@@SNIPEND
}
