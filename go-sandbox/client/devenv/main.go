// @@@SNIPSTART go-client-connect-env
package main

import (
	"fmt"
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/contrib/envconfig"
)

func main() {
	// Loads the "default" profile from the standard location and environment variables.
	c, err := client.Dial(envconfig.MustLoadDefaultClientOptions())
	if err != nil {
		log.Fatalf("Failed to create client: %v", err)
	}
	defer c.Close()

	fmt.Println("✅ Connected to Temporal Service")
}
// @@@SNIPEND
