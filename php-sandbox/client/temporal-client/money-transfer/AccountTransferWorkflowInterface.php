<?php // @@@SNIPSTART php-client-money-transfer-interface ?>
<?php
declare(strict_types=1);

namespace App\Client\TemporalClient\MoneyTransfer;

use Ramsey\Uuid\UuidInterface;
use Temporal\Workflow\ReturnType;
use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
interface AccountTransferWorkflowInterface
{
    #[WorkflowMethod(name: "MoneyTransfer")]
    #[ReturnType(UuidInterface::class)]
    public function transfer( string $fromAccountId, string $toAccountId, string $referenceId, int $amountCents);
}
// @@@SNIPEND
