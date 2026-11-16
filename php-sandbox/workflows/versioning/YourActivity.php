<?php
declare(strict_types=1);

namespace App\Workflows\Versioning;

class YourActivity implements YourActivityInterface
{
    public function prePatchActivity(): string
    {
        return 'pre-patch';
    }

    public function postPatchActivity(): string
    {
        return 'post-patch';
    }

    public function anotherPatchActivity(): string
    {
        return 'another-patch';
    }
}
