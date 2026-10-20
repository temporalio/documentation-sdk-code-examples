// @@@SNIPSTART go-client-cloud-profile
package main

import (
	"fmt"
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/contrib/envconfig"
)

func main() {
	// Replace with the actual path to your TOML file.
	configFilePath := "/Users/yourname/.config/my-app/temporal.toml"

	opts, err := envconfig.LoadClientOptions(envconfig.LoadClientOptionsRequest{
		ConfigFilePath:    configFilePath,
		ConfigFileProfile: "cloud",
	})
	if err != nil {
		log.Fatalf("Failed to load client config from custom file: %v", err)
	}

	c, err := client.Dial(opts)
	if err != nil {
		log.Fatalf("Failed to connect using custom config file: %v", err)
	}
	defer c.Close()

	fmt.Printf("✅ Connected using custom config at: %s\n", configFilePath)
}
// @@@SNIPEND
