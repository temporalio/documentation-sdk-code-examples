package main

import (
	"context"
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"
	"go.temporal.io/sdk/workflow"

	"my-org/quickstart/bestpractices/mycodecpackage"
)

func HelloWorkflow(ctx workflow.Context, name string) (string, error) {
	return "Hello " + name, nil
}

// main runs a Workflow with a Client whose Data Converter compresses every Payload.
func main() {
	// @@@SNIPSTART go-codec-client
	c, err := client.Dial(client.Options{
		// Set DataConverter here to ensure that Workflow inputs and results are
		// encoded as required.
		DataConverter: mycodecpackage.DataConverter,
	})
	// @@@SNIPEND
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	w := worker.New(c, "codec", worker.Options{})
	w.RegisterWorkflow(HelloWorkflow)
	if err := w.Start(); err != nil {
		log.Fatalln("Unable to start Worker", err)
	}
	defer w.Stop()

	run, err := c.ExecuteWorkflow(context.Background(), client.StartWorkflowOptions{ID: "codec-demo", TaskQueue: "codec"}, HelloWorkflow, "Temporal")
	if err != nil {
		log.Fatalln("Unable to start Workflow", err)
	}
	var result string
	if err := run.Get(context.Background(), &result); err != nil {
		log.Fatalln("Workflow failed", err)
	}
	log.Println("Workflow result:", result)
}
