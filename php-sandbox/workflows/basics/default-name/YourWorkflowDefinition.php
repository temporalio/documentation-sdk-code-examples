<?php
declare(strict_types=1);

namespace App\Workflows\Basics\DefaultName;

use App\Workflows\Basics\Argument;

class YourWorkflowDefinition implements YourWorkflowDefinitionInterface
{
    public function processFile(Argument $args)
    {
        return "Processed {$args->sourceUrl}";
    }
}
