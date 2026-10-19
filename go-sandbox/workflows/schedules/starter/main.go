package main

import (
	"context"
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"

	"my-org/quickstart/workflows/schedules"
)

// main runs each Schedule operation on this page in order, then deletes the Schedules it created.
func main() {
	ctx := context.Background()
	temporalClient, err := client.Dial(client.Options{
		HostPort: client.DefaultHostPort,
	})
	if err != nil {
		log.Fatalln("Unable to create Temporal Client", err)
	}
	defer temporalClient.Close()

	// Run a Worker so the Workflows that Schedules start can complete.
	w := worker.New(temporalClient, schedules.TaskQueue, worker.Options{})
	w.RegisterWorkflow(schedules.ScheduleWorkflow)
	w.RegisterWorkflow(schedules.YourWorkflowDefinition)
	if err := w.Start(); err != nil {
		log.Fatalln("Unable to start Worker", err)
	}
	defer w.Stop()

	if _, err := schedules.CreateSchedule(ctx, temporalClient); err != nil {
		log.Fatalln(err)
	}
	steps := []struct {
		name string
		run  func(context.Context, client.Client) error
	}{
		{"describe", func(ctx context.Context, c client.Client) error { _, err := schedules.DescribeSchedule(ctx, c); return err }},
		{"list", schedules.ListSchedules},
		{"pause", schedules.PauseSchedule},
		{"trigger", schedules.TriggerSchedule},
		{"update", schedules.UpdateSchedule},
		{"backfill", schedules.BackfillSchedule},
		{"delete", schedules.DeleteSchedule},
	}
	for _, step := range steps {
		if err := step.run(ctx, temporalClient); err != nil {
			log.Fatalln(step.name, err)
		}
	}

	// Delete the other Schedules this program created so it can run again.
	for _, id := range []string{"backfill-schedule", "test-schedule-describe-spec-cron-schedule", "trigger-schedule", "update-schedule"} {
		if err := temporalClient.ScheduleClient().GetHandle(ctx, id).Delete(ctx); err != nil {
			log.Println("Unable to delete schedule", id, err)
		}
	}
}
