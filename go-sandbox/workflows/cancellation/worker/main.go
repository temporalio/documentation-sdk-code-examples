package main

import (
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"

	"my-org/quickstart/workflows/cancellation"
)

func main() {
	temporalClient, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer temporalClient.Close()

	w := worker.New(temporalClient, cancellation.TaskQueueName, worker.Options{})
	w.RegisterWorkflow(cancellation.YourWorkflow)
	w.RegisterActivity(&cancellation.Activities{})

	if err := w.Run(worker.InterruptCh()); err != nil {
		log.Fatalln("Unable to start Worker", err)
	}
}
