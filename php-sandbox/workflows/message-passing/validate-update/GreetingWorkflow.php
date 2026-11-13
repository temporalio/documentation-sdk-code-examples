<?php // @@@SNIPSTART php-message-passing-validate-update ?>
<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\ValidateUpdate;

use Temporal\Workflow\ReturnType;
use Temporal\Workflow\UpdateMethod;
use Temporal\Workflow\UpdateValidatorMethod;
use Temporal\Workflow\WorkflowInterface;
use Temporal\Workflow\WorkflowMethod;

#[WorkflowInterface]
interface GreetingWorkflow {
    #[WorkflowMethod]
    #[ReturnType('array')]
    public function getGreetings();

    #[UpdateMethod]
    public function addGreeting(string $name): int;

    #[UpdateValidatorMethod(forUpdate: 'addGreeting')]
    public function addGreetingValidator(string $name): void;
}
// @@@SNIPEND
