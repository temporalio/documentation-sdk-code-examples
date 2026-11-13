<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\DefineUpdate;

use Temporal\Workflow;

class MyWorkflow implements MyWorkflowInterface
{
    private ?string $value = null;

    public function run()
    {
        yield Workflow::await(fn() => $this->value !== null);
        return $this->value;
    }

    public function myUpdate(string $value)
    {
        $this->value = $value;
        return "updated to {$value}";
    }
}
