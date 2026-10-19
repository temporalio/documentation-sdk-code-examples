package main

import (
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"

	"my-org/quickstart/workflows/messagepassing/message"
)

func main() {
	temporalClient, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer temporalClient.Close()

	w := worker.New(temporalClient, message.TaskQueue, worker.Options{})
	w.RegisterWorkflow(message.GreetingWorkflow)
	w.RegisterWorkflow(message.SignalAsyncWorkflow)
	w.RegisterWorkflow(message.DrainSignalsWorkflow)
	w.RegisterWorkflow(message.SendExternalSignalWorkflow)
	w.RegisterWorkflow(message.BlockingGreetingWorkflow)
	w.RegisterWorkflow(message.AwaitInHandlerWorkflow)
	w.RegisterWorkflow(message.WaitForHandlersWorkflow)
	w.RegisterWorkflow(message.UnfinishedPolicyWorkflow)
	w.RegisterWorkflow(message.BadHandlerWorkflow)
	w.RegisterWorkflow(message.SafeHandlerWorkflow)
	w.RegisterActivity(message.CallGreetingService)
	w.RegisterActivity(message.FetchData)

	if err := w.Run(worker.InterruptCh()); err != nil {
		log.Fatalln("Unable to start Worker", err)
	}
}
