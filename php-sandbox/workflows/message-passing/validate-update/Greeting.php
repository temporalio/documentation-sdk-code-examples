<?php
declare(strict_types=1);

namespace App\Workflows\MessagePassing\ValidateUpdate;

use Temporal\Workflow;

class Greeting implements GreetingWorkflow
{
    /** @var list<string> */
    private array $greetings = [];

    public function __construct()
    {
        // The Client page example calls this Update by name on an untyped stub.
        Workflow::registerUpdate('storeGreetings', fn() => yield Workflow::uuid());
    }

    public function getGreetings()
    {
        yield Workflow::await(fn() => \count($this->greetings) >= 2);
        return $this->greetings;
    }

    public function addGreeting(string $name): int
    {
        $this->greetings[] = "Hello, {$name}!";
        return \count($this->greetings);
    }

    public function addGreetingValidator(string $name): void
    {
        $name === '' and throw new \InvalidArgumentException('Name must not be empty');
    }
}
