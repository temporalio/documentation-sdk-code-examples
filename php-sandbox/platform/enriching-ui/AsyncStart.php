<?php
declare(strict_types=1);

namespace App\Platform\EnrichingUi;

use Temporal\Client\WorkflowClientInterface;

// The asynchronous start example from the Enriching the UI page.
final class AsyncStart
{
    public function start(WorkflowClientInterface $workflowClient, object $workflow): void
    {
        // @@@SNIPSTART php-enriching-ui-start-async
        // Start workflow asynchronously
        $workflowClient->start($workflow, 'workflow input');
        // @@@SNIPEND
    }
}
