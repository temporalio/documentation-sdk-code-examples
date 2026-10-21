package main

import (
	"context"
	"log"
	"os"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"

	"my-org/quickstart/bestpractices/externalstorage"
)

// main runs LargePayloadWorkflow with every Payload offloaded to a local disk store.
func main() {
	storeDir := os.Getenv("PAYLOAD_STORE_DIR")
	if storeDir == "" {
		storeDir = "/tmp/temporal-payload-store"
	}
	c, err := externalstorage.DialWithThreshold(externalstorage.NewLocalDiskStorageDriver(storeDir))
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	w := worker.New(c, "external-storage", worker.Options{})
	w.RegisterWorkflow(externalstorage.LargePayloadWorkflow)
	if err := w.Start(); err != nil {
		log.Fatalln("Unable to start Worker", err)
	}
	defer w.Stop()

	run, err := c.ExecuteWorkflow(context.Background(), client.StartWorkflowOptions{ID: "external-storage-demo", TaskQueue: "external-storage"},
		externalstorage.LargePayloadWorkflow, 300_000)
	if err != nil {
		log.Fatalln("Unable to start Workflow", err)
	}
	var result string
	if err := run.Get(context.Background(), &result); err != nil {
		log.Fatalln("Workflow failed", err)
	}
	log.Println("Workflow result length:", len(result))
}
