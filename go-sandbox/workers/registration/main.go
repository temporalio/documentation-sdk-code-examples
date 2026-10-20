package main

import (
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"
)

func main() {
	c, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	w := worker.New(c, "my-task-queue", worker.Options{})

	// @@@SNIPSTART go-worker-register-types
	w.RegisterWorkflow(WorkflowA)
	w.RegisterWorkflow(WorkflowB)
	w.RegisterActivity(&MyActivities{})
	// @@@SNIPEND

	err = w.Run(worker.InterruptCh())
	if err != nil {
		log.Fatalln("Unable to start worker", err)
	}
}
