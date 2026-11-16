<?php
declare(strict_types=1);

namespace App\Workers\RunWorkerProcess;

use Temporal\Activity\ActivityInterface;
use Temporal\Activity\ActivityMethod;

#[ActivityInterface]
class DemoActivityWithDependencies
{
    public function __construct(
        private readonly GreetingStore $store,
    ) {}

    #[ActivityMethod]
    public function store(string $greeting): string
    {
        return $this->store->save($greeting);
    }
}
