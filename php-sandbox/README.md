# PHP sandbox

Composer project for the [PHP SDK quickstart](https://docs.temporal.io/develop/php/set-up-your-local-php).

| File | Snippet |
|---|---|
| `src/GreetingActivity.php` | `php-quickstart-activity` |
| `src/SayHelloWorkflow.php` | `php-quickstart-workflow` |
| `worker.php` | `php-quickstart-worker` |
| `client.php` | `php-quickstart-client` |
| `.rr.yaml` | `php-quickstart-rr-yaml` |

## Run it

Requires PHP 8.2+ with the `grpc` extension, and the Temporal CLI.

```sh
composer install
./vendor/bin/rr get        # downloads the RoadRunner binary; keep the existing .rr.yaml
temporal server start-dev  # in another terminal
./rr serve                 # in another terminal; runs worker.php
php client.php             # prints "Result: Hello, Temporal!"
```

The PHP files start with `<?php // @@@SNIPSTART ... ?>` followed by a second
`<?php`, so the snippet on the docs page includes the opening tag. See
[Snipsync markers](../README.md#snipsync-markers).
