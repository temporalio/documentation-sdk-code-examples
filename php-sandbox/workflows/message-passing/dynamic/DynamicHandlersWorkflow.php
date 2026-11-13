<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\Dynamic;

use Temporal\DataConverter\ValuesInterface;
use Temporal\Workflow;

#[Workflow\WorkflowInterface]
class DynamicHandlersWorkflow
{
    private bool $finished = false;

    #[Workflow\WorkflowMethod]
    public function run()
    {
        // @@@SNIPSTART php-message-passing-dynamic-query
        Workflow::registerDynamicQuery(function (string $name, ValuesInterface $arguments): string {
            return \sprintf(
                'Got query `%s` with %d arguments',
                $name,
                $arguments->count(),
            );
        });
        // @@@SNIPEND

        // @@@SNIPSTART php-message-passing-dynamic-signal
        Workflow::registerDynamicSignal(function (string $name, ValuesInterface $arguments): void {
            Workflow::getLogger()->info(\sprintf(
                'Executed signal `%s` with %d arguments',
                $name,
                $arguments->count(),
            ));
        });
        // @@@SNIPEND

        // @@@SNIPSTART php-message-passing-dynamic-update
        Workflow::registerDynamicUpdate(
            static fn(string $name, ValuesInterface $arguments): string => \sprintf(
                'Got update `%s` with %d arguments',
                $name,
                $arguments->count(),
            ),
            static fn(string $name, ValuesInterface $arguments) => \str_starts_with(
                $name,
                'update_',
            ) or throw new \InvalidArgumentException('Invalid update name'),
        );
        // @@@SNIPEND

        yield Workflow::await(fn() => $this->finished);
    }

    #[Workflow\SignalMethod]
    public function finish(): void
    {
        $this->finished = true;
    }
}
