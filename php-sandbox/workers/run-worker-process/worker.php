<?php // @@@SNIPSTART php-run-worker-process ?>
<?php

declare(strict_types=1);

use App\Workers\RunWorkerProcess\Container;
use App\Workers\RunWorkerProcess\DemoActivity;
use App\Workers\RunWorkerProcess\DemoActivityWithDependencies;
use App\Workers\RunWorkerProcess\DemoWorkflow;
use Temporal\WorkerFactory;

ini_set('display_errors', 'stderr');
include "vendor/autoload.php";

// Your dependency injection container.
$container = new Container();

// factory initiates and runs task queue specific activity and workflow workers
$factory = WorkerFactory::create();

// Worker that listens on a Task Queue and hosts both workflow and activity implementations.
$worker = $factory->newWorker();

// Workflows are stateful. So you need a type to create instances.
$worker->registerWorkflowTypes(DemoWorkflow::class);

// Activities are stateless and thread safe. So a shared instance is used.
$worker->registerActivity(DemoActivity::class);

// In case an activity class requires some external dependencies provide a callback - factory
// that creates or builds a new activity instance. The factory should be a callable which accepts
// an instance of ReflectionClass with an activity class which should be created.
$worker->registerActivity(DemoActivityWithDependencies::class, fn(ReflectionClass $class) => $container->create($class->getName()));

// start primary loop
$factory->run();
// @@@SNIPEND
