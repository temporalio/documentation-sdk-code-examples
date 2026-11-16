<?php
// @@@SNIPSTART php-client-connect-api-key
$serviceClient = \Temporal\Client\GRPC\ServiceClient::createSSL('<your-custom-namespace>.tmprl.cloud:7233')
    ->withAuthKey('your-api-key');
// @@@SNIPEND
