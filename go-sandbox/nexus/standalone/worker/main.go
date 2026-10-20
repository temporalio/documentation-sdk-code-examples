package main

import (
	"log"

	"github.com/nexus-rpc/sdk-go/nexus"
	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/contrib/envconfig"
	"go.temporal.io/sdk/worker"

	"my-org/quickstart/nexus/handler"
	"my-org/quickstart/nexus/service"
)

const taskQueue = "nexus-handler-queue"

// main runs the Nexus handler Worker for Standalone Nexus Operations.
// It uses the same Service and Operation handlers as Workflow-driven Operations.
func main() {
	c, err := client.Dial(envconfig.MustLoadDefaultClientOptions())
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	w := worker.New(c, taskQueue, worker.Options{})
	helloService := nexus.NewService(service.HelloServiceName)
	if err := helloService.Register(handler.EchoOperation, handler.HelloOperation); err != nil {
		log.Fatalln("Unable to register operations", err)
	}
	w.RegisterNexusService(helloService)
	w.RegisterWorkflow(handler.HelloHandlerWorkflow)

	if err := w.Run(worker.InterruptCh()); err != nil {
		log.Fatalln("Unable to start worker", err)
	}
}
