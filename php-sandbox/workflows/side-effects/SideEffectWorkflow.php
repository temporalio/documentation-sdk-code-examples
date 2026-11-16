<?php
declare(strict_types=1);

namespace App\Workflows\SideEffects;

use Temporal\Workflow;

#[Workflow\WorkflowInterface]
class SideEffectWorkflow
{
    // @@@SNIPSTART php-side-effects
    #[Workflow\WorkflowMethod]
    public function run()
    {
        $random = yield Workflow::sideEffect(fn() => random_int(0, 100));
        if ($random < 50) {
            // ...
        } else {
            // ...
        }
    }
    // @@@SNIPEND
}
