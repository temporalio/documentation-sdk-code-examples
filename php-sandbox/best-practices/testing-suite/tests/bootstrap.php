<?php // @@@SNIPSTART php-testing-bootstrap ?>
<?php
declare(strict_types=1);

require __DIR__ . '/../vendor/autoload.php';

use Temporal\Testing\Environment;

$environment = Environment::create();
$environment->startTemporalTestServer();
$environment->startRoadRunner(['./rr', 'serve', '-c', '.rr.test.yaml', '-w', 'tests'], configFile: '.rr.test.yaml');
register_shutdown_function(fn () => $environment->stop());
// @@@SNIPEND
