// @@@SNIPSTART go-client-override
package main

import (
	"fmt"
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/contrib/envconfig"
)

func main() {
	// Load the base configuration (e.g., from the default profile).
	opts := envconfig.MustLoadDefaultClientOptions()

	// Apply overrides programmatically.
	opts.HostPort = "localhost:7233"
	opts.Namespace = "test-namespace"

	c, err := client.Dial(opts)
	if err != nil {
		log.Fatalf("Failed to connect with overridden options: %v", err)
	}
	defer c.Close()

	fmt.Printf("✅ Connected with overridden config to: %s in namespace: %s\n", opts.HostPort, opts.Namespace)
}
// @@@SNIPEND
