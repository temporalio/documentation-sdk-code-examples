package main

import (
	"context"
	"log"
	"os"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"

	"my-org/quickstart/nexus/caller"
	"my-org/quickstart/nexus/options"
	"my-org/quickstart/nexus/service"
)

// main runs the Approve and multi-argument Operations from the caller Namespace.
// Start an approval Workflow in the handler Namespace first, with the ID that handler.GetWorkflowID returns.
func main() {
	clientOptions, err := options.ParseClientOptionFlags(os.Args[1:])
	if err != nil {
		log.Fatalf("Invalid arguments: %v", err)
	}
	c, err := client.Dial(clientOptions)
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	w := worker.New(c, "my-caller-approve-task-queue", worker.Options{})
	w.RegisterWorkflow(caller.ApproveCallerWorkflow)
	w.RegisterWorkflow(caller.HelloWithArgsCallerWorkflow)
	if err := w.Start(); err != nil {
		log.Fatalln("Unable to start worker", err)
	}
	defer w.Stop()

	ctx := context.Background()
	opts := client.StartWorkflowOptions{TaskQueue: "my-caller-approve-task-queue"}
	var greeting string
	run, err := c.ExecuteWorkflow(ctx, opts, caller.HelloWithArgsCallerWorkflow, "Nexus", service.FR)
	if err == nil {
		err = run.Get(ctx, &greeting)
	}
	if err != nil {
		log.Fatalln("HelloWithArgsCallerWorkflow failed", err)
	}
	log.Println("Workflow result:", greeting)

	run, err = c.ExecuteWorkflow(ctx, opts, caller.ApproveCallerWorkflow, "user-1")
	if err == nil {
		err = run.Get(ctx, nil)
	}
	if err != nil {
		log.Fatalln("ApproveCallerWorkflow failed", err)
	}
	log.Println("Approved user-1")
}
