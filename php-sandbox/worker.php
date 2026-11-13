<?php // @@@SNIPSTART php-quickstart-worker ?>
<?php
declare(strict_types=1);

use Temporal\WorkerFactory;

ini_set('display_errors', 'stderr');
require "vendor/autoload.php";

$factory = WorkerFactory::create();

$worker = $factory->newWorker();

// Register Workflows
$worker->registerWorkflowTypes(\App\SayHelloWorkflow::class);
// Register Activities
$worker->registerActivity(\App\GreetingActivity::class);

$factory->run();
// @@@SNIPEND
