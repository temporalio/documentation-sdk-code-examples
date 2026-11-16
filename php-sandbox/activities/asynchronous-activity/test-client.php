<?php
declare(strict_types=1);

// Runs the Client code from the Asynchronous Activity Completion page against a
// fake Temporal Service, without the grpc extension. A fake gRPC stub stands in
// for the connection, and an interceptor records each request the SDK sends.
// Run from php-sandbox/:
//   php activities/asynchronous-activity/test-client.php

use App\Activities\AsynchronousActivity\ActivityCompletion;
use Temporal\Client\GRPC\ContextInterface;
use Temporal\Client\GRPC\ServiceClient;
use Temporal\Client\WorkflowClient;
use Temporal\DataConverter\DataConverter;
use Temporal\DataConverter\EncodedValues;
use Temporal\Interceptor\GrpcClientInterceptor;
use Temporal\Internal\Interceptor\Pipeline;

require __DIR__ . '/../../vendor/autoload.php';

$recorder = new class implements GrpcClientInterceptor {
    /** @var array<string, object> Last request per RPC method */
    public array $requests = [];

    public function interceptCall(string $method, object $arg, ContextInterface $ctx, callable $next): object
    {
        $this->requests[$method] = $arg;
        return new ("Temporal\\Api\\Workflowservice\\V1\\{$method}Response")();
    }
};
$serviceClient = (new ServiceClient(static fn() => new class extends \Grpc\BaseStub {
    public function __construct() {}

    public function close(): void {}
}))->withInterceptorPipeline(Pipeline::prepare([$recorder]));
$client = new WorkflowClient($serviceClient);
$completion = new ActivityCompletion();

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

$taskToken = random_bytes(32);

// Complete by Task Token. GreetingActivity logs the token base64-encoded.
$completion->complete($client, base64_encode($taskToken), 'Hello World');
$request = $recorder->requests['RespondActivityTaskCompleted'];
check('complete token', $taskToken, $request->getTaskToken());
check('complete result', ['Hello World'], EncodedValues::fromPayloads($request->getResult(), DataConverter::createDefault())->getValues());

// Fail by Task Token.
$completion->fail($client->newActivityCompletionClient(), $taskToken);
$request = $recorder->requests['RespondActivityTaskFailed'];
check('fail token', $taskToken, $request->getTaskToken());
check('fail message', 'activity failed', $request->getFailure()->getMessage());

exit($failures > 0 ? 1 : 0);
