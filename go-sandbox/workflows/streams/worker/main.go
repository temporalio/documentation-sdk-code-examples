package main

import (
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"

	"my-org/quickstart/workflows/streams"
	"my-org/quickstart/workflows/streams/chat"
)

func main() {
	temporalClient, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer temporalClient.Close()

	w := worker.New(temporalClient, streams.TaskQueue, worker.Options{})
	w.RegisterWorkflow(streams.OrderWorkflow)
	w.RegisterWorkflow(streams.LongRunningWorkflow)
	w.RegisterWorkflow(chat.ChatWorkflow)
	w.RegisterActivity(streams.ValidateOrder)
	w.RegisterActivity(streams.ChargePayment)
	w.RegisterActivity(streams.DispatchOrder)
	w.RegisterActivity(streams.StreamDeltas)
	w.RegisterActivity(chat.StreamCompletion)

	if err := w.Run(worker.InterruptCh()); err != nil {
		log.Fatalln("Unable to start Worker", err)
	}
}
