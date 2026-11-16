<?php
declare(strict_types=1);

namespace App\Client\TemporalClient\Updates;

use Ramsey\Uuid\UuidInterface;
use Temporal\Workflow;

class AccountTransferWorkflow implements AccountTransferWorkflowInterface
{
    /** @var list<string> */
    private array $moves = [];
    private ?string $outcome = null;

    public function begin(UuidInterface $transactionId)
    {
        yield Workflow::await(fn() => $this->outcome !== null);
        return ['transaction' => $transactionId->toString(), 'moves' => $this->moves, 'outcome' => $this->outcome];
    }

    public function move(UuidInterface $from, UuidInterface $to, int $amount)
    {
        $this->moves[] = "{$amount} {$from->toString()} -> {$to->toString()}";
        return \count($this->moves);
    }

    public function commit()
    {
        $this->outcome = 'committed';
    }

    public function rollback(string $reason)
    {
        $this->outcome = "rolled back: {$reason}";
    }
}
