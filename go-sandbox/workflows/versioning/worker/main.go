package main

import (
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"

	"my-org/quickstart/workflows/versioning"
	"my-org/quickstart/workflows/versioning/pizza"
)

func main() {
	temporalClient, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer temporalClient.Close()

	w := worker.New(temporalClient, versioning.TaskQueue, worker.Options{})
	// @@@SNIPSTART go-versioning-cutover-register
	w.RegisterWorkflow(pizza.PizzaWorkflow)
	w.RegisterWorkflow(pizza.PizzaWorkflowV2)
	// @@@SNIPEND

	if err := w.Run(worker.InterruptCh()); err != nil {
		log.Fatalln("Unable to start Worker", err)
	}
}
