<?php // @@@SNIPSTART php-workflow-basics-argument ?>
<?php
declare(strict_types=1);

namespace App\Workflows\Basics;

final class Argument
{
    public function __construct(
        public string $sourceUrl = '',
        public string $destinationUrl = '',
    ) {}
}
// @@@SNIPEND
