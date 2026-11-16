<?php
declare(strict_types=1);

// Runs the Workflow from the Side Effects page on the side-effects Task Queue.

use App\Workflows\SideEffects\SideEffectWorkflow;
use Temporal\WorkerFactory;

ini_set('display_errors', 'stderr');
require __DIR__ . '/../../vendor/autoload.php';

$factory = WorkerFactory::create();
$factory->newWorker('side-effects')->registerWorkflowTypes(SideEffectWorkflow::class);
$factory->run();
