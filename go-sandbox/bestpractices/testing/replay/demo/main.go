package main

import (
	"context"
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"

	"my-org/quickstart/bestpractices/testing/replay"
)

// main runs YourWorkflow on a local Temporal Service, then replays its Event History.
func main() {
	c, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	w := worker.New(c, "replay", worker.Options{})
	w.RegisterWorkflow(replay.YourWorkflow)
	if err := w.Start(); err != nil {
		log.Fatalln("Unable to start Worker", err)
	}
	defer w.Stop()

	ctx := context.Background()
	run, err := c.ExecuteWorkflow(ctx, client.StartWorkflowOptions{TaskQueue: "replay"}, replay.YourWorkflow, "Temporal")
	if err != nil {
		log.Fatalln("Unable to start Workflow", err)
	}
	if err := run.Get(ctx, nil); err != nil {
		log.Fatalln("Workflow failed", err)
	}

	if err := replay.ReplayWorkflow(ctx, c, run.GetID(), run.GetRunID()); err != nil {
		log.Fatalln("Replay failed", err)
	}
	log.Println("Replayed", run.GetID(), "with no nondeterminism errors")
}
