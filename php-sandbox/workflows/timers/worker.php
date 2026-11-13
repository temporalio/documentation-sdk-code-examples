<?php
declare(strict_types=1);

// Runs the Workflow from the Timers page on the timers Task Queue.

use App\Workflows\Timers\TimerWorkflow;
use Temporal\WorkerFactory;

ini_set('display_errors', 'stderr');
require __DIR__ . '/../../vendor/autoload.php';

$factory = WorkerFactory::create();
$factory->newWorker('timers')->registerWorkflowTypes(TimerWorkflow::class);
$factory->run();
