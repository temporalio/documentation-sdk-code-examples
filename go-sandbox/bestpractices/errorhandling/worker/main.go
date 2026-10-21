package main

import (
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"

	"my-org/quickstart/bestpractices/errorhandling"
)

func main() {
	c, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	w := worker.New(c, "error-handling", worker.Options{})
	w.RegisterWorkflow(errorhandling.HandleErrorsWorkflow)
	w.RegisterActivity(errorhandling.YourActivity)
	if err := w.Run(worker.InterruptCh()); err != nil {
		log.Fatalln("Unable to start Worker", err)
	}
}
