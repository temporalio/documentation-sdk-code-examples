<?php
declare(strict_types=1);

require __DIR__ . '/../../../vendor/autoload.php';

use App\BestPractices\TestingSuite\SimpleActivity as MyActivity;
use App\BestPractices\TestingSuite\SimpleWorkflow as MyWorkflow;

// @@@SNIPSTART php-testing-mock-worker
// worker.test.php
use Temporal\Testing\WorkerFactory;

$factory = WorkerFactory::create();
$worker = $factory->newWorker();

$worker->registerWorkflowTypes(MyWorkflow::class);
$worker->registerActivity(MyActivity::class);
$factory->run();
// @@@SNIPEND
