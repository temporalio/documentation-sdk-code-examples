<?php
declare(strict_types=1);

namespace App\Workers\RunWorkerProcess;

use Temporal\Worker\ServiceCredentials;
use Temporal\Worker\WorkerFactoryInterface;
use Temporal\Worker\WorkerInterface;
use Temporal\Worker\WorkerOptions;

// The Worker code from the "Run a Worker" page other than worker.php. Each
// method holds one example; its parameters are the variables it uses.
// examples-worker.php runs them under RoadRunner.
final class WorkerExamples
{
    public function apiKeyFactory(): WorkerFactoryInterface
    {
        // @@@SNIPSTART php-run-worker-api-key
        $workerFactory = \Temporal\WorkerFactory::create(
            credentials: ServiceCredentials::create()->withApiKey('your-api-key'),
        );
        // @@@SNIPEND

        return $workerFactory;
    }

    public function taskQueue(WorkerFactoryInterface $factory): WorkerInterface
    {
        // @@@SNIPSTART php-run-worker-task-queue
        $worker = $factory->newWorker('your-task-queue');
        // @@@SNIPEND

        return $worker;
    }

    public function workerOptions(WorkerFactoryInterface $factory): WorkerInterface
    {
        // @@@SNIPSTART php-run-worker-options
        $worker = $factory->newWorker(
            'your-task-queue',
            WorkerOptions::new()
                ->withMaxConcurrentWorkflowTaskPollers(10)
        );
        // @@@SNIPEND

        return $worker;
    }

    public function registerTypes(WorkerInterface $worker): void
    {
        // @@@SNIPSTART php-run-worker-register-types
        // Workflows are stateful. So you need a type to create instances:
        $worker->registerWorkflowTypes(DemoWorkflow::class);
        // Activities are stateless and thread safe:
        $worker->registerActivity(DemoActivity::class);
        // @@@SNIPEND
    }

    public function registerActivityFactory(WorkerInterface $worker, Container $container): void
    {
        // @@@SNIPSTART php-run-worker-activity-factory
        $worker->registerActivity(
            DemoActivityWithDependencies::class,
            fn(\ReflectionClass $class) => $container->create($class->getName())
        );
        // @@@SNIPEND
    }

    public function registerFinalizer(WorkerInterface $worker, Kernel $kernel): void
    {
        // @@@SNIPSTART php-run-worker-finalizer
        $worker->registerActivityFinalizer(fn() => $kernel->shutdown());
        // @@@SNIPEND
    }
}
