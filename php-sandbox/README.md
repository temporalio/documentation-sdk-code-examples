# PHP sandbox

Composer project for the PHP SDK docs code samples. The project root holds the
[PHP SDK quickstart](https://docs.temporal.io/develop/php/set-up-your-local-php).
Samples for other pages live in a folder per page, such as `workflows/basics/`,
and are autoloaded through the `classmap` entry in `composer.json`.

## Quickstart

| File | Snippet |
|---|---|
| `src/GreetingActivity.php` | `php-quickstart-activity` |
| `src/SayHelloWorkflow.php` | `php-quickstart-workflow` |
| `worker.php` | `php-quickstart-worker` |
| `client.php` | `php-quickstart-client` |
| `.rr.yaml` | `php-quickstart-rr-yaml` |

### Run it

Requires PHP 8.2+ with the `grpc` extension, and the Temporal CLI.

```sh
composer install
./vendor/bin/rr get        # downloads the RoadRunner binary; keep the existing .rr.yaml
temporal server start-dev  # in another terminal
./rr serve                 # in another terminal; runs worker.php
php client.php             # prints "Result: Hello, Temporal!"
```

## Workflow basics

Samples for [Workflow basics](https://docs.temporal.io/develop/php/workflows/basics)
in `workflows/basics/`.

| File | Snippet |
|---|---|
| `workflows/basics/define/FileProcessingWorkflow.php` | `php-workflow-basics-define` |
| `workflows/basics/Argument.php` | `php-workflow-basics-argument` |
| `workflows/basics/return-values/FileProcessingWorkflow.php` | `php-workflow-basics-return-type` |
| `workflows/basics/custom-name/FileProcessingWorkflow.php` | `php-workflow-basics-custom-name` |
| `workflows/basics/default-name/YourWorkflowDefinitionInterface.php` | `php-workflow-basics-default-name` |
| `workflows/basics/workflow-init/GreetingExample.php` | `php-workflow-basics-workflow-init` |

`workflows/basics/worker.php` registers each Workflow on its own Task Queue.
`workflows/basics/test.sh` starts a dev server and RoadRunner, runs each
Workflow with the Temporal CLI, and checks the result. It needs `jq` and the
`rr` binary in this folder.

```sh
workflows/basics/test.sh
```

## Child Workflows

Samples for [Child Workflows](https://docs.temporal.io/develop/php/workflows/child-workflows)
in `workflows/child-workflows/`.

| File | Snippet |
|---|---|
| `workflows/child-workflows/StubParentWorkflow.php` | `php-child-workflows-stub` |
| `workflows/child-workflows/ExecuteParentWorkflow.php` | `php-child-workflows-execute` |
| `workflows/child-workflows/AbandonParentWorkflow.php` | `php-child-workflows-parent-close-policy` |

`ChildWorkflow.php` and `LongRunningChildWorkflow.php` are the children the
parents start. `worker.php` registers them all on the `child-workflows` Task
Queue, and `test.sh` runs each parent the same way as the Workflow basics test.

```sh
workflows/child-workflows/test.sh
```

## Continue-As-New

Samples for [Continue-As-New](https://docs.temporal.io/develop/php/workflows/continue-as-new)
in `workflows/continue-as-new/`, a smaller version of the samples-php
`SafeMessageHandlers` cluster manager without Activities.

| File | Snippet |
|---|---|
| `workflows/continue-as-new/ClusterManagerInput.php` | `php-continue-as-new-input` |
| `workflows/continue-as-new/MessageHandlerWorkflowInterface.php` | `php-continue-as-new-interface` |
| `workflows/continue-as-new/MessageHandlerWorkflow.php` | `php-continue-as-new-call`, `php-continue-as-new-should-continue-as-new` |

`test.sh` starts the Workflow with `testContinueAsNew`, sends Updates until it
continues as new, and checks that the state carries over into the new run.

```sh
workflows/continue-as-new/test.sh
```

## Workflow timeouts

Samples for [Workflow Timeouts](https://docs.temporal.io/develop/php/workflows/timeouts)
in `workflows/timeouts/`.

| File | Snippet |
|---|---|
| `workflows/timeouts/WorkflowStarter.php` | `php-workflow-timeouts-execution-timeout`, `php-workflow-timeouts-retry-policy` |

`test.php` checks the options the SDK sends when it starts each Workflow. It
doesn't need a Temporal Service or the `grpc` extension: a fake gRPC stub stands
in for the connection, and an interceptor records each request.

```sh
php workflows/timeouts/test.php
```

## Cancellation

Samples for [Cancellation](https://docs.temporal.io/develop/php/workflows/cancellation)
in `workflows/cancellation/`.

| File | Snippet |
|---|---|
| `workflows/cancellation/CancelActivityWorkflow.php` | `php-cancellation-cancel-activity` |
| `workflows/cancellation/MyActivity.php` | `php-cancellation-activity` |

`test.sh` runs the Workflow and checks that the Activity is canceled, runs its
cleanup, and that the Workflow waits for it.

```sh
workflows/cancellation/test.sh
```

## Workflow message passing

Samples for [Workflow message passing](https://docs.temporal.io/develop/php/workflows/message-passing)
in `workflows/message-passing/`, one folder per example. Client code is in
`client/MessagePassingClient.php`. Snippet names start with `php-message-passing-`.

`test.sh` runs every Workflow on a dev server and sends Signals, Queries, and
Updates with the Temporal CLI. `test-client.php` runs the Client code against a
fake Temporal Service, the same way as the Workflow timeouts test, with canned
Workflow, Query, and Update results.

```sh
workflows/message-passing/test.sh
php -d zend.assertions=1 workflows/message-passing/test-client.php
```

## Snipsync markers

The PHP files start with `<?php // @@@SNIPSTART ... ?>` followed by a second
`<?php`, so the snippet on the docs page includes the opening tag. See
[Snipsync markers](../README.md#snipsync-markers).
