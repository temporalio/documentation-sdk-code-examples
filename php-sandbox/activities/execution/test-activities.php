<?php
declare(strict_types=1);

// Calls the S3 Activity implementation from the Activity Execution page with a
// real AWS SDK S3Client whose HTTP handler is the SDK's MockHandler, so no
// AWS account or network is needed. Checks the S3 requests the Activities
// build and the local file operations. Run from php-sandbox/:
//   php activities/execution/test-activities.php

use App\Activities\Execution\FileProcessingActivitiesImpl;
use Aws\CommandInterface;
use Aws\MockHandler;
use Aws\Result;
use Aws\S3\S3Client;

require __DIR__ . '/../../vendor/autoload.php';

$mock = new MockHandler();
$s3Client = new S3Client([
    'region' => 'us-east-1',
    'credentials' => ['key' => 'test', 'secret' => 'test'],
    'handler' => $mock,
]);

$dir = sys_get_temp_dir() . '/activity-execution-' . bin2hex(random_bytes(4)) . '/';
mkdir($dir);
file_put_contents($dir . 'report.txt', str_repeat('Temporal ', 100));

$activities = new FileProcessingActivitiesImpl($s3Client, $dir);

$failures = 0;
function check(string $name, mixed $expected, mixed $actual): void
{
    global $failures;
    if ($expected === $actual) {
        echo "ok   {$name}\n";
    } else {
        echo "FAIL {$name}: expected " . var_export($expected, true) . ', got ' . var_export($actual, true) . "\n";
        $failures++;
    }
}

// upload
$mock->append(new Result([]));
$activities->upload('my-bucket', 'report.txt', 'reports/report.txt');
$command = $mock->getLastCommand();
check('upload command', 'PutObject', $command->getName());
check('upload bucket', 'my-bucket', $command['Bucket']);
check('upload key', 'reports/report.txt', $command['Key']);
rewind($command['Body']);
check('upload body', str_repeat('Temporal ', 100), stream_get_contents($command['Body']));

// download
$mock->append(new Result([]));
$activities->download('my-bucket', 'reports/other.txt', 'other.txt');
$command = $mock->getLastCommand();
check('download command', 'GetObject', $command->getName());
check('download key', 'reports/other.txt', $command['Key']);
// The SDK turns SaveAs into the HTTP sink option.
check('download save path', $dir . 'other.txt', $command['@http']['sink'] ?? $command['SaveAs']);

// processFile
$compressed = $activities->processFile('report.txt');
check('processFile path', $dir . 'report.txt.gz', $compressed);
check('processFile content', str_repeat('Temporal ', 100), gzdecode(file_get_contents($compressed)));

// deleteLocalFile
$activities->deleteLocalFile('report.txt');
check('deleteLocalFile', false, file_exists($dir . 'report.txt'));

unlink($compressed);
rmdir($dir);
exit($failures > 0 ? 1 : 0);
