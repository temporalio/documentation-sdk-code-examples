<?php
declare(strict_types=1);

namespace App\Client\TemporalClient;

use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
interface YourWorkflowInterface
{
    #[WorkflowMethod]
    public function transfer(string $fromAccountId, string $toAccountId, string $referenceId, int $amountCents);
}
