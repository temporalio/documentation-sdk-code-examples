<?php
declare(strict_types=1);

namespace App\Platform\Observability;

use Temporal\Client\WorkflowClientInterface;
use Temporal\Client\WorkflowOptions;
use Temporal\Common\SearchAttributes\SearchAttributeKey;
use Temporal\Common\TypedSearchAttributes;

// Client code from the Observability page. Each method holds one example; its
// parameters are the variables the example uses.
final class ClientExamples
{
    public function listWorkflows(WorkflowClientInterface $workflowClient): void
    {
        // @@@SNIPSTART php-observability-list-workflows
        $paginator = $workflowClient->listWorkflowExecutions('WorkflowType="GreetingWorkflow"');

        foreach ($paginator as $info) {
            echo "Workflow ID: {$info->execution->getID()}\n";
        }
        // @@@SNIPEND
    }

    public function startWithSearchAttributes(WorkflowClientInterface $workflowClient, string $orderid): object
    {
        // @@@SNIPSTART php-observability-set-search-attributes
        $keyDestinationTime = SearchAttributeKey::forDatetime('DestinationTime');
        $keyOrderId = SearchAttributeKey::forKeyword('OrderId');

        $workflow = $workflowClient->newWorkflowStub(
            OrderWorkflowInterface::class,
            WorkflowOptions::new()
                ->withWorkflowExecutionTimeout('10 minutes')
                ->withTypedSearchAttributes(
                    TypedSearchAttributes::empty()
                        ->withValue($keyOrderId, $orderid)
                        ->withValue($keyDestinationTime, new \DateTimeImmutable('2028-11-05T00:10:07Z'))
                ),
        );
        // @@@SNIPEND

        return $workflow;
    }
}
