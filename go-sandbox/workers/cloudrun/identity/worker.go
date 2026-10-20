package main

import (
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"

	"my-org/quickstart/workers/cloudrun/myapp"
)

func runWorker(c client.Client) {
	w := worker.New(c, "cloud-run-task-queue", worker.Options{})
	w.RegisterWorkflow(myapp.MyWorkflow)
	w.RegisterActivity(myapp.MyActivity)
	if err := w.Run(worker.InterruptCh()); err != nil {
		log.Fatalln("Unable to start worker", err)
	}
}
