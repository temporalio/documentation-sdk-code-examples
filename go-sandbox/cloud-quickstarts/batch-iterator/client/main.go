package main

import (
	"context"
	"fmt"
	"log"
	"time"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/contrib/envconfig"

	batchiterator "my-org/quickstart/cloud-quickstarts/batch-iterator"
)

func main() {
	// Same environment-configuration lookup the Worker uses: TEMPORAL_PROFILE
	// selects the profile, the TOML file supplies address/namespace/credentials,
	// and TEMPORAL_* env vars can override individual fields.
	opts, err := envconfig.LoadDefaultClientOptions()
	if err != nil {
		log.Fatalln("Unable to load client options", err)
	}
	if opts.HostPort == "" {
		opts.HostPort = client.DefaultHostPort
	}
	if opts.Namespace == "" {
		opts.Namespace = client.DefaultNamespace
	}

	c, err := client.Dial(opts)
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	run, err := c.ExecuteWorkflow(context.Background(), client.StartWorkflowOptions{
		ID:        fmt.Sprintf("batch-iterator-%d", time.Now().UnixMilli()),
		TaskQueue: batchiterator.TaskQueue,
	}, batchiterator.IteratorBatchWorkflow, batchiterator.PageSize, 0)
	if err != nil {
		log.Fatalln("Unable to start Workflow", err)
	}
	log.Printf("Started batch Workflow %s against %s (namespace: %s)...",
		run.GetID(), opts.HostPort, opts.Namespace)

	// Get follows the continue-as-new chain and returns the final run's result.
	var processed int
	if err := run.Get(context.Background(), &processed); err != nil {
		log.Fatalln("Batch failed", err)
	}
	log.Printf("Batch complete: processed %d records", processed)
}
