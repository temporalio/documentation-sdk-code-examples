// Package cloud shows Client options for connecting to Temporal Cloud.
// endpoint is the gRPC endpoint, such as "<namespace>.<account>.tmprl.cloud:7233",
// and namespace is the Namespace and Account ID, such as "<namespace_id>.<account_id>".
package cloud

import (
	"context"
	"crypto/tls"

	"go.temporal.io/sdk/client"
)

// DialWithAPIKey connects to Temporal Cloud with an API key.
func DialWithAPIKey(endpoint, namespace, apiKey string) (client.Client, error) {
	// @@@SNIPSTART go-client-cloud-api-key
	clientOptions := client.Options{
		HostPort:          endpoint,  // <namespace>.<account>.tmprl.cloud:7233
		Namespace:         namespace, // <namespace_id>.<account_id>
		ConnectionOptions: client.ConnectionOptions{TLS: &tls.Config{}},
		Credentials:       client.NewAPIKeyStaticCredentials(apiKey),
	}
	c, err := client.Dial(clientOptions)
	// @@@SNIPEND
	return c, err
}

// DynamicAPIKeyCredentials returns credentials that read the current API key on each request,
// then replaces the key. Requests made after the update use myKeyUpdated.
func DynamicAPIKeyCredentials(initialKey, myKeyUpdated string) client.Credentials {
	// @@@SNIPSTART go-client-cloud-api-key-dynamic
	// Assuming client Credentials created with
	myKey := initialKey
	creds := client.NewAPIKeyDynamicCredentials(
		func(context.Context) (string, error) { return myKey, nil })
	// Update by replacing
	myKey = myKeyUpdated
	// @@@SNIPEND
	return creds
}

// DialWithRotatingCertificate connects with mTLS and reloads the certificate on every new connection.
func DialWithRotatingCertificate(endpoint, namespace, clientCertPath, clientKeyPath string) (client.Client, error) {
	// @@@SNIPSTART go-client-cloud-mtls-rotation
	clientOptions := client.Options{
		HostPort:  endpoint,  // <namespace>.<account>.tmprl.cloud:7233
		Namespace: namespace, // <namespace_id>.<account_id>
		ConnectionOptions: client.ConnectionOptions{
			TLS: &tls.Config{
				GetClientCertificate: func(*tls.CertificateRequestInfo) (*tls.Certificate, error) {
					cert, err := tls.LoadX509KeyPair(clientCertPath, clientKeyPath)
					if err != nil {
						return nil, err
					}
					return &cert, nil
				},
			},
		},
	}
	c, err := client.Dial(clientOptions)
	// @@@SNIPEND
	return c, err
}
