<?php // @@@SNIPSTART php-client-workflow-interface ?>
<?php
declare(strict_types=1);

namespace App\Client\TemporalClient\Updates;

use Ramsey\Uuid\UuidInterface;
use Temporal\Workflow\UpdateMethod;
use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
interface AccountTransferWorkflowInterface
{
    #[WorkflowMethod(name: "account.transfer")]
    public function begin(UuidInterface $transactionId);

    #[UpdateMethod(name: "pay")]
    public function move(UuidInterface $from, UuidInterface $to, int $amount);

    #[UpdateMethod(name: "finish")]
    public function commit();

    #[UpdateMethod(name: "cancel")]
    public function rollback(string $reason);
}
// @@@SNIPEND
