<?php
declare(strict_types=1);

// Runs the Workflows defined on the Temporal Client page on the
// temporal-client Task Queue.

use App\Client\TemporalClient\MoneyTransfer;
use App\Client\TemporalClient\Updates;
use Temporal\WorkerFactory;

ini_set('display_errors', 'stderr');
require __DIR__ . '/../../vendor/autoload.php';

$factory = WorkerFactory::create();
$factory->newWorker('temporal-client')->registerWorkflowTypes(
    Updates\AccountTransferWorkflow::class,
    MoneyTransfer\AccountTransferWorkflow::class,
);
$factory->run();
