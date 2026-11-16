<?php
declare(strict_types=1);

require __DIR__ . '/../vendor/autoload.php';

use Temporal\Testing\Environment;

// @@@SNIPSTART php-testing-bootstrap-optional-server
if (getenv('RUN_TEMPORAL_TEST_SERVER') !== false) {
    $environment = Environment::create();
    $environment->startTemporalTestServer();
    $environment->startRoadRunner(['./rr', 'serve', '-c', '.rr.silent.yaml', '-w', 'tests'], configFile: '.rr.silent.yaml');
    register_shutdown_function(fn() => $environment->stop());
}
// @@@SNIPEND
