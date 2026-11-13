<?php
declare(strict_types=1);

namespace App\Workflows\Basics\CustomName;

use App\Workflows\Basics\Argument;

class FileProcessing implements FileProcessingWorkflow
{
    public function processFile(Argument $args)
    {
        return "Processed {$args->sourceUrl}";
    }
}
