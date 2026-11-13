<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\UpdateWithStart;

use Temporal\Workflow;

class ShoppingCart implements ShoppingCartWorkflow
{
    private const UNIT_PRICE = 10;

    /** @var array<string, int> Item ID => quantity */
    private array $items = [];
    private bool $checkedOut = false;

    public function run()
    {
        yield Workflow::await(fn() => $this->checkedOut);
        yield Workflow::await(fn() => Workflow::allHandlersFinished());
        return $this->items;
    }

    public function addItem(string $itemId, int $quantity): int
    {
        $this->items[$itemId] = ($this->items[$itemId] ?? 0) + $quantity;
        return \array_sum($this->items) * self::UNIT_PRICE;
    }

    public function checkout(): void
    {
        $this->checkedOut = true;
    }
}
