<?php
declare(strict_types=1);

namespace App\Client\TemporalClient\MoneyTransfer;

use Temporal\Workflow;

class AccountTransferWorkflow implements AccountTransferWorkflowInterface
{
    public function transfer(string $fromAccountId, string $toAccountId, string $referenceId, int $amountCents)
    {
        // Returns a confirmation ID for the transfer.
        return yield Workflow::uuid();
    }
}
