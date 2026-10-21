package main

import (
	"context"
	"log"

	"github.com/google/uuid"
	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/workflow"

	"my-org/quickstart/bestpractices/ctxpropagation"
)

func main() {
	// @@@SNIPSTART go-ctx-propagation-starter
	// The client is a heavyweight object that should be created once per process.
	c, err := client.Dial(client.Options{
		HostPort:           client.DefaultHostPort,
		ContextPropagators: []workflow.ContextPropagator{ctxpropagation.NewContextPropagator()},
	})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	workflowID := "ctx-propagation_" + uuid.NewString()
	workflowOptions := client.StartWorkflowOptions{
		ID:        workflowID,
		TaskQueue: "ctx-propagation",
	}

	ctx := context.Background()
	ctx = context.WithValue(ctx, ctxpropagation.PropagateKey, &ctxpropagation.Values{Key: "test", Value: "tested"})

	we, err := c.ExecuteWorkflow(ctx, workflowOptions, ctxpropagation.CtxPropWorkflow)
	// @@@SNIPEND
	if err != nil {
		log.Fatalln("Unable to execute workflow", err)
	}
	log.Println("Started workflow", "WorkflowID", we.GetID(), "RunID", we.GetRunID())
	if err := we.Get(ctx, nil); err != nil {
		log.Fatalln("Workflow failed", err)
	}
	log.Println("Workflow completed")
}
