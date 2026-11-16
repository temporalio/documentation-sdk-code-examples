<?php // @@@SNIPSTART php-testing-activity-interface ?>
<?php
declare(strict_types=1);

namespace App\BestPractices\TestingSuite;

use Temporal\Activity\ActivityInterface;
use Temporal\Activity\ActivityMethod;

#[ActivityInterface(prefix: "SimpleActivity.")]
interface SimpleActivityInterface
{
    #[ActivityMethod('doSomething')]
    public function doSomething(string $input): string;
}
// @@@SNIPEND
