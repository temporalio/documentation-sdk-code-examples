package main

import (
	"context"
	"log"
	"slices"
	"time"

	"go.temporal.io/sdk/client"

	"my-org/quickstart/client/namespaces"
)

// main registers a Namespace on a local Temporal Service, then describes, updates, lists, and deletes it.
func main() {
	ctx := context.Background()
	name := "your-namespace-name"

	if err := namespaces.RegisterNamespace(ctx, name); err != nil {
		log.Fatalln("Unable to register namespace", err)
	}
	log.Println("Registered namespace", name)

	temporalClient, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer temporalClient.Close()
	namespaceClient, err := client.NewNamespaceClient(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create namespace client", err)
	}
	defer namespaceClient.Close()

	// Registration can take up to 10 seconds to complete.
	var listed []string
	for i := 0; i < 20; i++ {
		if listed, err = namespaces.ListNamespaces(ctx, temporalClient); err == nil && slices.Contains(listed, name) {
			break
		}
		time.Sleep(time.Second)
	}
	log.Println("Namespaces:", listed)

	if err := namespaces.UpdateNamespace(ctx, namespaceClient); err != nil {
		log.Fatalln("Unable to update namespace", err)
	}
	updated, err := namespaceClient.Describe(ctx, name)
	if err != nil {
		log.Fatalln("Unable to describe namespace", err)
	}
	log.Println("Updated description:", updated.GetNamespaceInfo().GetDescription())

	description, err := namespaces.DescribeNamespace(ctx)
	if err != nil {
		log.Fatalln("Unable to describe default namespace", err)
	}
	log.Println("default retention:", description.GetConfig().GetWorkflowExecutionRetentionTtl().AsDuration())

	if err := namespaces.DeleteNamespace(ctx, temporalClient, name); err != nil {
		log.Fatalln("Unable to delete namespace", err)
	}
	log.Println("Deleted namespace", name)
}
