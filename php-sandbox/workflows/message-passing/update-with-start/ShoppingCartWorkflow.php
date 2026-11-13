<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\UpdateWithStart;

use Temporal\Workflow\SignalMethod;
use Temporal\Workflow\UpdateMethod;
use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
interface ShoppingCartWorkflow
{
    #[WorkflowMethod]
    public function run();

    #[UpdateMethod]
    public function addItem(string $itemId, int $quantity): int;

    #[SignalMethod]
    public function checkout(): void;
}
