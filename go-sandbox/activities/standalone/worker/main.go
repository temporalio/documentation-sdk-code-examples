package main

import (
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"

	"my-org/quickstart/activities/standalone"
)

func main() {
	c, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	// Registering a Standalone Activity is the same as registering any other Activity.
	w := worker.New(c, standalone.TaskQueue, worker.Options{})
	w.RegisterActivity(standalone.Activity)

	if err := w.Run(worker.InterruptCh()); err != nil {
		log.Fatalln("Unable to start Worker", err)
	}
}
