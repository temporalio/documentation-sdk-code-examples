<?php
declare(strict_types=1);

namespace App\Platform\Observability;

use DateTimeImmutable;
use Temporal\Common\SearchAttributes\SearchAttributeKey;
use Temporal\Workflow;

class OrderWorkflow implements OrderWorkflowInterface
{
    private bool $delivered = false;

    public function run()
    {
        yield Workflow::await(fn() => $this->delivered);
    }

    #[Workflow\SignalMethod]
    public function deliver(): void
    {
        $this->delivered = true;
    }

    // @@@SNIPSTART php-observability-upsert-search-attributes
    #[Workflow\UpdateMethod]
    public function postponeDestinationTime(string $interval)
    {
        // Get the key for the DestinationTime attribute
        $keyDestinationTime = SearchAttributeKey::forDatetime('DestinationTime');

        /** @var DateTimeImmutable $destinationTime */
        $destinationTime = Workflow::getInfo()->typedSearchAttributes->get($keyDestinationTime);

        Workflow::upsertTypedSearchAttributes(
            // $interval is an ISO 8601 duration, such as "PT1H" for one hour.
            $keyDestinationTime->valueSet($destinationTime->add(new \DateInterval($interval))),
        );
    }
    // @@@SNIPEND

    // @@@SNIPSTART php-observability-remove-search-attribute
    #[Workflow\UpdateMethod]
    public function unsetDestinationTime()
    {
        // Get the key for the DestinationTime attribute
        $keyDestinationTime = SearchAttributeKey::forDatetime('DestinationTime');

        Workflow::upsertTypedSearchAttributes(
            $keyDestinationTime->valueUnset(),
        );
    }
    // @@@SNIPEND
}
