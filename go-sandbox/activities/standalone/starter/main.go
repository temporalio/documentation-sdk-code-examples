package main

import (
	"context"
	"flag"
	"log"
	"time"

	"go.temporal.io/sdk/client"

	"my-org/quickstart/activities/standalone"
)

func main() {
	command := flag.String("command", "start", "one of: start, handle, list, count")
	runID := flag.String("run-id", "", "Run ID of the Activity Execution for the handle command")
	flag.Parse()

	c, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	switch *command {
	case "start":
		startActivity(c)
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
	handle, err := c.ExecuteActivity(context.Background(), client.StartActivityOptions{
		ID:                  standalone.ActivityID,
		TaskQueue:           standalone.TaskQueue,
		StartToCloseTimeout: 10 * time.Second,
	}, standalone.Activity, "Temporal")
	if err != nil {
		log.Fatalln("Unable to execute activity", err)
	}

	// @@@SNIPSTART go-standalone-activity-get-result
	var result string
	err = handle.Get(context.Background(), &result)
	if err != nil {
		log.Fatalln("Activity failed", err)
	}
	log.Println("Activity result:", result)
	// @@@SNIPEND
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
