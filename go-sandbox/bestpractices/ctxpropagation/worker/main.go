package main

import (
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"
	"go.temporal.io/sdk/workflow"

	"my-org/quickstart/bestpractices/ctxpropagation"
)

func main() {
	// The Worker's Client needs the same propagator so Workflows and Activities can read the values.
	c, err := client.Dial(client.Options{
		ContextPropagators: []workflow.ContextPropagator{ctxpropagation.NewContextPropagator()},
	})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	w := worker.New(c, ctxpropagation.TaskQueue, worker.Options{})
	w.RegisterWorkflow(ctxpropagation.CtxPropWorkflow)
	w.RegisterActivity(ctxpropagation.SampleActivity)

	if err := w.Run(worker.InterruptCh()); err != nil {
		log.Fatalln("Unable to start Worker", err)
	}
}
