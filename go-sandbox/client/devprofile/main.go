// @@@SNIPSTART go-client-connect-profile
package main

import (
	"fmt"
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/contrib/envconfig"
)

func main() {
	// Load a specific profile from the TOML config file.
	// This requires a [profile.prod] section in your config.
	opts, err := envconfig.LoadClientOptions(envconfig.LoadClientOptionsRequest{
		ConfigFileProfile: "prod",
	})
	if err != nil {
		log.Fatalf("Failed to load 'prod' profile: %v", err)
	}

	c, err := client.Dial(opts)
	if err != nil {
		log.Fatalf("Failed to connect using 'prod' profile: %v", err)
	}
	defer c.Close()

	fmt.Printf("✅ Connected to Temporal namespace %q on %s using 'prod' profile\n", opts.Namespace, opts.HostPort)
}
// @@@SNIPEND
