<?php
declare(strict_types=1);

// Runs one revision of MyWorkflow from the Versioning page. Every revision has
// the Workflow Type MyWorkflow, so a Worker registers only one of them. Choose
// it with the VERSIONING_REVISION environment variable: pre-patch, patched,
// another-patch, min-supported-1, or min-supported-2.

use App\Workflows\Versioning;
use App\Workflows\Versioning\Cutover\MyWorkflow;
use App\Workflows\Versioning\Cutover\MyWorkflowV2;
use Temporal\WorkerFactory;

ini_set('display_errors', 'stderr');
require __DIR__ . '/../../vendor/autoload.php';

$revisions = [
    'pre-patch' => Versioning\PrePatch\MyWorkflow::class,
    'patched' => Versioning\Patched\MyWorkflow::class,
    'another-patch' => Versioning\AnotherPatch\MyWorkflow::class,
    'min-supported-1' => Versioning\MinSupported1\MyWorkflow::class,
    'min-supported-2' => Versioning\MinSupported2\MyWorkflow::class,
];
$revision = getenv('VERSIONING_REVISION') ?: 'pre-patch';

$factory = WorkerFactory::create();

$worker = $factory->newWorker('versioning');
$worker->registerWorkflowTypes($revisions[$revision] ?? throw new \InvalidArgumentException("Unknown revision {$revision}"));
$worker->registerActivity(Versioning\YourActivity::class);

$worker = $factory->newWorker('versioning-cutover');
// @@@SNIPSTART php-versioning-cutover-worker
$worker->registerWorkflowTypes(MyWorkflow::class, MyWorkflowV2::class);
// @@@SNIPEND

$factory->run();
