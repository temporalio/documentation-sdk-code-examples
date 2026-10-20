package main

import (
	"context"
	"log"
	"time"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/contrib/envconfig"

	"my-org/quickstart/nexus/service"
)

// main executes Standalone Nexus Operations from client code, without a Workflow,
// then lists and counts them.
func main() {
	// The client is a heavyweight object that should be created once per process.
	c, err := client.Dial(envconfig.MustLoadDefaultClientOptions())
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()
	ctx := context.Background()

	echoHandle, err := executeOperation(ctx, c, service.EchoOperationName, service.EchoInput{Message: "hello"}, "nexus-standalone-echo-op")
	if err != nil {
		log.Fatalln("Unable to execute Echo operation", err)
	}
	getEchoResult(echoHandle)

	helloHandle, err := executeOperation(ctx, c, service.HelloOperationName, service.HelloInput{Name: "Temporal", Language: service.EN}, "nexus-standalone-hello-op")
	if err != nil {
		log.Fatalln("Unable to execute Hello operation", err)
	}
	var helloResult service.HelloOutput
	if err := helloHandle.Get(ctx, &helloResult); err != nil {
		log.Fatalln("Hello operation failed", err)
	}
	log.Println("Operation result:", helloResult.Message)

	listOperations(c)
	countOperations(c)
}

// executeOperation starts a Standalone Nexus Operation and returns its handle.
func executeOperation(ctx context.Context, c client.Client, operationName string, input any, operationID string) (client.NexusOperationHandle, error) {
	// @@@SNIPSTART go-nexus-standalone-execute
	nexusClient, err := c.NewNexusClient(client.NexusClientOptions{
		Endpoint: "my-nexus-endpoint",
		Service:  "my-hello-service",
	})
	if err != nil {
		return nil, err
	}

	handle, err := nexusClient.ExecuteOperation(ctx, operationName, input, client.StartNexusOperationOptions{
		ID:                     operationID, // A unique Operation ID, such as "my-echo-op"
		ScheduleToCloseTimeout: 10 * time.Second,
	})
	// @@@SNIPEND
	return handle, err
}

func getEchoResult(handle client.NexusOperationHandle) {
	// @@@SNIPSTART go-nexus-standalone-get-result
	var result service.EchoOutput
	err := handle.Get(context.Background(), &result)
	if err != nil {
		log.Fatalln("Operation failed", err)
	}
	log.Println("Operation result:", result.Message)
	// @@@SNIPEND
}

func listOperations(c client.Client) {
	// @@@SNIPSTART go-nexus-standalone-list
	resp, err := c.ListNexusOperations(context.Background(), client.ListNexusOperationsOptions{
		Query: "Endpoint = 'my-nexus-endpoint'",
	})
	if err != nil {
		log.Fatalln("Unable to list Nexus operations", err)
	}

	for metadata, err := range resp.Results {
		if err != nil {
			log.Fatalln("Error iterating operations", err)
		}
		log.Printf("OperationID: %s, Operation: %s, Status: %v\n",
			metadata.OperationID, metadata.Operation, metadata.Status)
	}
	// @@@SNIPEND
}

func countOperations(c client.Client) {
	// @@@SNIPSTART go-nexus-standalone-count
	resp, err := c.CountNexusOperations(context.Background(), client.CountNexusOperationsOptions{
		Query: "Endpoint = 'my-nexus-endpoint'",
	})
	if err != nil {
		log.Fatalln("Unable to count Nexus operations", err)
	}

	log.Println("Total Nexus operations:", resp.Count)
	// @@@SNIPEND
}
