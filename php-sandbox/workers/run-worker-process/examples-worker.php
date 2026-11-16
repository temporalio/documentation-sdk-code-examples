<?php
declare(strict_types=1);

// Builds a Worker from the WorkerExamples methods, the way an application
// would combine them, and runs it under RoadRunner. Used by test.sh.

use App\Workers\RunWorkerProcess\Container;
use App\Workers\RunWorkerProcess\Kernel;
use App\Workers\RunWorkerProcess\WorkerExamples;

ini_set('display_errors', 'stderr');
require __DIR__ . '/../../vendor/autoload.php';

$examples = new WorkerExamples();

// The API key factory: RoadRunner sends the key with each request. The dev
// server ignores it.
$factory = $examples->apiKeyFactory();

$worker = $examples->workerOptions($factory);
$examples->registerTypes($worker);
$examples->registerActivityFactory($worker, new Container());
$examples->registerFinalizer($worker, new Kernel());

// A second Task Queue on the same Worker Process.
$another = $factory->newWorker('another-task-queue');
$examples->registerTypes($another);
$examples->registerActivityFactory($another, new Container());

$factory->run();
