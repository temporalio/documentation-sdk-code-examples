<?php
declare(strict_types=1);

namespace App\BestPractices\TestingSuite\Tests;

use App\BestPractices\TestingSuite\SimpleWorkflow;
use PHPUnit\Framework\TestCase;
use Temporal\Client\GRPC\ServiceClient;
use Temporal\Client\WorkflowClient;
use Temporal\Exception\Client\WorkflowFailedException;
use Temporal\Testing\ActivityMocker;

// Needs the grpc extension, a Temporal Service at localhost:7233, and
// worker.test.php running under RoadRunner with .rr.test.yaml.
final class SimpleWorkflowFailureTestCase extends TestCase
{
    private WorkflowClient $workflowClient;
    private ActivityMocker $activityMocks;

    protected function setUp(): void
    {
        $this->workflowClient = new WorkflowClient(ServiceClient::create('localhost:7233'));
        $this->activityMocks = new ActivityMocker();

        parent::setUp();
    }

    protected function tearDown(): void
    {
        $this->activityMocks->clear();
        parent::tearDown();
    }

    public function testWorkflowFailsWhenActivityFails(): void
    {
        // @@@SNIPSTART php-testing-mock-activity-failure
        $this->activityMocks->expectFailure('SimpleActivity.doSomething', new \LogicException('something went wrong'));
        // @@@SNIPEND
        $workflow = $this->workflowClient->newWorkflowStub(SimpleWorkflow::class);
        $run = $this->workflowClient->start($workflow, 'hello');

        $this->expectException(WorkflowFailedException::class);
        $run->getResult('string');
    }
}
