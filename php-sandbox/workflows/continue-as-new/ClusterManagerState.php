<?php
declare(strict_types=1);

namespace App\Workflows\ContinueAsNew;

final class ClusterManagerState
{
    public function __construct(
        public bool $clusterStarted = false,
        public bool $clusterShutdown = false,
        /** @var array<int, string|null> Node number => assigned job name */
        public array $nodes = [],
        /** @var list<string> */
        public array $jobsAssigned = [],
    ) {}
}
