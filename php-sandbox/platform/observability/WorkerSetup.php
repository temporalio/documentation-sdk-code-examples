<?php
declare(strict_types=1);

namespace App\Platform\Observability;

use Temporal\Worker\WorkerOptions;
use Temporal\WorkerFactory;

// Worker setup from the Observability page. Each example creates its own
// WorkerFactory, and a PHP process runs one factory, so worker.php and
// worker-custom-logger.php run them separately.
final class WorkerSetup
{
    public function loggingInReplay(): array
    {
        // @@@SNIPSTART php-observability-logging-in-replay
        $factory = WorkerFactory::create();
        $worker = $factory->newWorker('your-task-queue', WorkerOptions::new()
            ->withEnableLoggingInReplay(true)
        );
        // @@@SNIPEND

        return [$factory, $worker];
    }

    public function customLogger(): array
    {
        // @@@SNIPSTART php-observability-custom-logger
        $myLogger = new MyLogger();

        $workerFactory = WorkerFactory::create();
        $worker = $workerFactory->newWorker(
            taskQueue: 'my-task-queue',
            logger: $myLogger,
        );
        // @@@SNIPEND

        return [$workerFactory, $worker];
    }
}
