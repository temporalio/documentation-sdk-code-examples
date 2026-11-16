<?php
declare(strict_types=1);

namespace App\Workers\RunWorkerProcess;

// A dependency that DemoActivityWithDependencies needs, such as a database client.
class GreetingStore
{
    public function save(string $greeting): string
    {
        return "stored: {$greeting}";
    }
}
