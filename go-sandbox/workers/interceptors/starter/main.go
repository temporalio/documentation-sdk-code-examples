package main

import (
	"context"
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"
	"go.temporal.io/sdk/workflow"

	"my-org/quickstart/workers/interceptors"
)

func HelloWorkflow(ctx workflow.Context) error { return nil }

// main starts a Workflow with a Client that has the logging interceptor.
func main() {
	c, err := interceptors.Dial()
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	w := worker.New(c, "interceptors", worker.Options{})
	w.RegisterWorkflow(HelloWorkflow)
	if err := w.Start(); err != nil {
		log.Fatalln("Unable to start Worker", err)
	}
	defer w.Stop()

	run, err := c.ExecuteWorkflow(context.Background(), client.StartWorkflowOptions{
		ID:        "interceptor-demo",
		TaskQueue: "interceptors",
	}, HelloWorkflow)
	if err != nil {
		log.Fatalln("Unable to execute workflow", err)
	}
	if err := run.Get(context.Background(), nil); err != nil {
		log.Fatalln("Workflow failed", err)
	}
}
