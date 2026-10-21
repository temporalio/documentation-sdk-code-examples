// @@@SNIPSTART go-nexus-quickstart-caller-main
package main

import (
	"context"
	"log"

	"my-org/quickstart"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"
)

func main() {
	c, err := client.Dial(client.Options{
		Namespace: "my-caller-namespace",
	})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	w := worker.New(c, quickstart.CallerTaskQueue, worker.Options{})
	w.RegisterWorkflow(quickstart.CallerWorkflow)

	if err := w.Start(); err != nil {
		log.Fatalln("Unable to start worker", err)
	}
	defer w.Stop()

	wr, err := c.ExecuteWorkflow(context.Background(), client.StartWorkflowOptions{
		ID:        "nexus-caller-workflow",
		TaskQueue: quickstart.CallerTaskQueue,
	}, quickstart.CallerWorkflow, "Temporal")
	if err != nil {
		log.Fatalln("Unable to execute workflow", err)
	}

	var result string
	if err := wr.Get(context.Background(), &result); err != nil {
		log.Fatalln("Unable to get workflow result", err)
	}
	log.Println("Workflow result:", result)
}
// @@@SNIPEND
