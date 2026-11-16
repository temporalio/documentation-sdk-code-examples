<?php
declare(strict_types=1);

namespace App\Workers\RunWorkerProcess;

// Stands in for an application kernel that holds resources, such as database
// connections, to release after each Activity.
class Kernel
{
    public function shutdown(): void
    {
        \error_log('Kernel shut down');
    }
}
