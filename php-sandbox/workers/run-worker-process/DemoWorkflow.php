<?php
declare(strict_types=1);

namespace App\Workers\RunWorkerProcess;

use Temporal\Activity\ActivityOptions;
use Temporal\Workflow;

#[Workflow\WorkflowInterface]
class DemoWorkflow
{
    #[Workflow\WorkflowMethod]
    public function run(string $name)
    {
        $options = ActivityOptions::new()->withStartToCloseTimeout(10);
        $greeting = yield Workflow::newActivityStub(DemoActivity::class, $options)->greet($name);
        $stored = yield Workflow::newActivityStub(DemoActivityWithDependencies::class, $options)->store($greeting);
        return [$greeting, $stored];
    }
}
