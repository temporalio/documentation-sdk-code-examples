<?php // @@@SNIPSTART php-quickstart-activity ?>
<?php
declare(strict_types=1);

namespace App;

use Temporal\Activity\ActivityInterface;
use Temporal\Activity\ActivityMethod;

#[ActivityInterface]
class GreetingActivity
{
    #[ActivityMethod]
    public function greet(string $name): string
    {
        return "Hello, $name!";
    }
}
// @@@SNIPEND
