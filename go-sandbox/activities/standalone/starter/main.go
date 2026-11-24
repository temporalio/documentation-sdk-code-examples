package main

import (
	"context"
	"flag"
	"log"
	"time"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/contrib/envconfig"

	"my-org/quickstart/activities/standalone"
)

func main() {
	command := flag.String("command", "start", "one of: start, start-by-name, handle, list, count")
	runID := flag.String("run-id", "", "Run ID of the Activity Execution for the handle command")
	flag.Parse()

	c, err := client.Dial(envconfig.MustLoadDefaultClientOptions())
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	switch *command {
	case "start":
		startActivity(c)
	case "start-by-name":
		startActivityByName(c)
	case "handle":
		getActivityHandle(c, *runID)
	case "list":
		listActivities(c)
	case "count":
		countActivities(c)
	default:
		log.Fatalln("Unknown command", *command)
	}
}

func startActivity(c client.Client) {
	// @@@SNIPSTART go-standalone-activity-quickstart-execute
	options := client.StartActivityOptions{
		ID:                  standalone.ActivityID,
		TaskQueue:           standalone.TaskQueue,
		StartToCloseTimeout: 10 * time.Second,
	}

	handle, err := c.ExecuteActivity(context.Background(), options, standalone.Activity, "Temporal")
	if err != nil {
		log.Fatalln("Unable to execute activity", err)
	}
	log.Println("Started Standalone Activity", "ActivityID", handle.GetID(), "RunID", handle.GetRunID())
	// @@@SNIPEND

	// @@@SNIPSTART go-standalone-activity-get-result
	var result string
	err = handle.Get(context.Background(), &result)
	if err != nil {
		log.Fatalln("Activity failed", err)
	}
	log.Println("Activity result:", result)
	// @@@SNIPEND
}

// startActivityByName starts the same Activity by its registered type name instead of
// a function reference. Use this when the caller can't import the Activity function.
func startActivityByName(c client.Client) {
	options := client.StartActivityOptions{
		ID:                  standalone.ActivityID,
		TaskQueue:           standalone.TaskQueue,
		StartToCloseTimeout: 10 * time.Second,
	}

	// @@@SNIPSTART go-standalone-activity-quickstart-execute-by-name
	handle, err := c.ExecuteActivity(context.Background(), options, "Activity", "Temporal")
	// @@@SNIPEND
	if err != nil {
		log.Fatalln("Unable to execute activity", err)
	}

	var result string
	if err := handle.Get(context.Background(), &result); err != nil {
		log.Fatalln("Activity failed", err)
	}
	log.Println("Activity result:", result)
}

func getActivityHandle(c client.Client, runID string) {
	// @@@SNIPSTART go-standalone-activity-get-handle
	handle := c.GetActivityHandle(client.GetActivityHandleOptions{
		ActivityID: "standalone_activity_helloworld_ActivityID",
		RunID:      runID, // Leave empty to target the latest Activity Execution.
	})

	// Use the handle to get the result, describe, cancel, or terminate
	var result string
	err := handle.Get(context.Background(), &result)
	if err != nil {
		log.Fatalln("Unable to get activity result", err)
	}
	// @@@SNIPEND
	log.Println("Activity result:", result)
}

func listActivities(c client.Client) {
	// @@@SNIPSTART go-standalone-activity-list
	resp, err := c.ListActivities(context.Background(), client.ListActivitiesOptions{
		Query: "TaskQueue = 'standalone-activity-helloworld'",
	})
	if err != nil {
		log.Fatalln("Unable to list activities", err)
	}

	for info, err := range resp.Results {
		if err != nil {
			log.Fatalln("Error iterating activities", err)
		}
		log.Printf("ActivityID: %s, Type: %s, Status: %v\n",
			info.ActivityID, info.ActivityType, info.Status)
	}
	// @@@SNIPEND
}

func countActivities(c client.Client) {
	// @@@SNIPSTART go-standalone-activity-count
	resp, err := c.CountActivities(context.Background(), client.CountActivitiesOptions{
		Query: "TaskQueue = 'standalone-activity-helloworld'",
	})
	if err != nil {
		log.Fatalln("Unable to count activities", err)
	}

	log.Println("Total activities:", resp.Count)
	// @@@SNIPEND
}
