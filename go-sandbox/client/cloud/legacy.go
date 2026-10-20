package cloud

import (
	"context"
	"crypto/tls"
	"log"

	"go.temporal.io/sdk/client"
	"google.golang.org/grpc"
	"google.golang.org/grpc/metadata"
)

// DialWithAPIKeyV126 is the API key connection for Go SDK v1.26.0 to v1.32.x,
// which needed the temporal-namespace header added to each request.
func DialWithAPIKeyV126(endpoint, namespace, apiKey string) client.Client {
	// @@@SNIPSTART go-client-cloud-api-key-v126
	clientOptions := client.Options{
		HostPort:  endpoint,  // <namespace>.<account>.tmprl.cloud:7233
		Namespace: namespace, // <namespace_id>.<account_id>
		ConnectionOptions: client.ConnectionOptions{
			TLS: &tls.Config{},
			DialOptions: []grpc.DialOption{
				grpc.WithUnaryInterceptor(
					func(ctx context.Context, method string, req any, reply any, cc *grpc.ClientConn, invoker grpc.UnaryInvoker, opts ...grpc.CallOption) error {
						return invoker(
							metadata.AppendToOutgoingContext(ctx, "temporal-namespace", namespace),
							method,
							req,
							reply,
							cc,
							opts...,
						)
					},
				),
			},
		},
		Credentials: client.NewAPIKeyStaticCredentials(apiKey),
	}
	c, err := client.Dial(clientOptions)
	if err != nil {
		log.Fatalf("error creating temporal client: %v", err)
	}
	// @@@SNIPEND
	return c
}

// @@@SNIPSTART go-client-cloud-api-key-headers-provider
// Create headers provider
type APIKeyProvider struct {
	APIKey    string
	Namespace string
}

func (a *APIKeyProvider) GetHeaders(context.Context) (map[string]string, error) {
	return map[string]string{"Authorization": "Bearer " + a.APIKey, "temporal-namespace": a.Namespace}, nil
}

// DialWithHeadersProvider is the API key connection for Go SDK versions before v1.26.0.
func DialWithHeadersProvider(endpoint, namespace, apiKey string) (client.Client, *APIKeyProvider, error) {
	// Use headers provider
	apiKeyProvider := &APIKeyProvider{APIKey: apiKey, Namespace: namespace}
	c, err := client.Dial(client.Options{
		HostPort:          endpoint,  // <namespace>.<account>.tmprl.cloud:7233
		Namespace:         namespace, // <namespace_id>.<account_id>
		HeadersProvider:   apiKeyProvider,
		ConnectionOptions: client.ConnectionOptions{TLS: &tls.Config{}},
	})
	return c, apiKeyProvider, err
}
// @@@SNIPEND

// UpdateHeadersProviderKey replaces the API key used by a headers provider.
func UpdateHeadersProviderKey(apiKeyProvider *APIKeyProvider, myKeyUpdated string) {
	// @@@SNIPSTART go-client-cloud-api-key-headers-provider-update
	apiKeyProvider.APIKey = myKeyUpdated
	// @@@SNIPEND
}
