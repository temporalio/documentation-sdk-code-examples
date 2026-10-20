// @@@SNIPSTART go-serverless-cloud-run-identity
package main

import (
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/contrib/envconfig"
	"go.temporal.io/sdk/contrib/gcp/cloudrun/id"
)

func main() {
	clientOptions, err := envconfig.LoadDefaultClientOptions()
	if err != nil {
		log.Fatalln("Unable to load Temporal client options", err)
	}
	clientOptions.Plugins = append(clientOptions.Plugins, id.NewCloudRunIDPlugin())
	c, err := client.Dial(clientOptions)
	if err != nil {
		log.Fatalln("Unable to create Temporal client", err)
	}
	defer c.Close()

	// Workers created from this Client use the Cloud Run identity.
	runWorker(c)
}
// @@@SNIPEND
