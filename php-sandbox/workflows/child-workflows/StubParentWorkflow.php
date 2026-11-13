<?php
declare(strict_types=1);

namespace App\Workflows\ChildWorkflows;

use DateInterval;
use Temporal\Exception\TemporalException;
use Temporal\Workflow;
use Temporal\Workflow\ChildWorkflowOptions;

#[Workflow\WorkflowInterface]
class StubParentWorkflow
{
    #[Workflow\WorkflowMethod]
    #[Workflow\ReturnType('string')]
    public function run()
    {
        // @@@SNIPSTART php-child-workflows-stub
        // Use one stub per child workflow run
        $child = Workflow::newChildWorkflowStub(
            ChildWorkflowInterface::class,
            ChildWorkflowOptions::new()
                // Do not specify WorkflowId if you want Temporal to generate a unique Id
                // for the child execution.
                ->withWorkflowId('BID-SIMPLE-CHILD-WORKFLOW')
                ->withWorkflowExecutionTimeout(DateInterval::createFromDateString('30 minutes'))
        );

        // This is a non blocking call that returns immediately.
        // Use yield $child->workflowMethod(name) to call synchronously.
        $promise = $child->workflowMethod('value');

        // Do something else here.
        try {
            $value = yield $promise;
        } catch (TemporalException $e) {
            Workflow::getLogger()->error('child workflow failed');
            throw $e;
        }
        // @@@SNIPEND

        return $value;
    }
}
