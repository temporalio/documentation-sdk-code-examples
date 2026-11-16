<?php
declare(strict_types=1);

namespace App\Workflows\Versioning;

use Temporal\Activity\ActivityInterface;
use Temporal\Activity\ActivityMethod;

#[ActivityInterface]
interface YourActivityInterface
{
    #[ActivityMethod]
    public function prePatchActivity(): string;

    #[ActivityMethod]
    public function postPatchActivity(): string;

    #[ActivityMethod]
    public function anotherPatchActivity(): string;
}
