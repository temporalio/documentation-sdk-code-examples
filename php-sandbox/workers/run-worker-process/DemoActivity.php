<?php
declare(strict_types=1);

namespace App\Workers\RunWorkerProcess;

use Temporal\Activity\ActivityInterface;
use Temporal\Activity\ActivityMethod;

#[ActivityInterface]
class DemoActivity
{
    #[ActivityMethod]
    public function greet(string $name): string
    {
        return "Hello, {$name}!";
    }
}
