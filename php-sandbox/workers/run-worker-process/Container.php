<?php
declare(strict_types=1);

namespace App\Workers\RunWorkerProcess;

// Stands in for a dependency injection container, such as the ones in Symfony
// or Laravel, that builds objects and their dependencies.
class Container
{
    public function create(string $class): object
    {
        return match ($class) {
            DemoActivityWithDependencies::class => new DemoActivityWithDependencies(new GreetingStore()),
            default => new $class(),
        };
    }
}
