package main

import (
	"context"
	"log"
	"time"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"

	"my-org/quickstart/platform/observability"
)

// main connects with each logger and metrics option on the Observability page,
// runs the logging Workflow, starts a Workflow with Search Attributes, and lists open Workflows.
// Create the custom Search Attributes CustomerId (Keyword) and MiscData (Text) first.
func main() {
	for name, dial := range map[string]func() (client.Client, error){
		"slog": observability.DialWithSlog, "zap": observability.DialWithZap,
		"tracing": observability.DialWithTracing, "monotonic counters": observability.DialWithMonotonicCounters,
	} {
		c, err := dial()
		if err != nil {
			log.Fatalf("Unable to connect with %s: %v", name, err)
		}
		c.Close()
		log.Println("Connected with", name)
	}

	c, err := client.Dial(observability.PrometheusClientOptions())
	if err != nil {
		log.Fatalln("Unable to connect with Prometheus metrics", err)
	}
	defer c.Close()

	w := worker.New(c, "observability", worker.Options{})
	w.RegisterWorkflow(observability.Workflow)
	w.RegisterWorkflow(observability.YourWorkflow)
	w.RegisterActivity(observability.Activity)
	if err := w.Start(); err != nil {
		log.Fatalln("Unable to start Worker", err)
	}
	defer w.Stop()

	ctx := context.Background()
	run, err := c.ExecuteWorkflow(ctx, client.StartWorkflowOptions{TaskQueue: "observability"}, observability.Workflow, "Temporal")
	if err != nil {
		log.Fatalln("Unable to start Workflow", err)
	}
	var result string
	if err := run.Get(ctx, &result); err != nil {
		log.Fatalln("Workflow failed", err)
	}
	log.Println("Workflow result:", result)

	app := &observability.Client{Client: c}
	if err := app.CallYourWorkflow(ctx, "search-attributes-demo", map[string]interface{}{"customer": "customer-42", "miscData": "yellow"}); err != nil {
		log.Fatalln("Unable to start Workflow with Search Attributes", err)
	}
	time.Sleep(2 * time.Second) // Visibility is eventually consistent.
	if err := observability.ListOpenWorkflows(c); err != nil {
		log.Fatalln("Unable to list Workflows", err)
	}
	time.Sleep(2 * time.Second) // Let the metrics reporter flush before the program exits.
}
