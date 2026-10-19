package schedules

import (
	"context"
	"fmt"
	"log"
	"time"

	"github.com/google/uuid"
	enums "go.temporal.io/api/enums/v1"
	"go.temporal.io/sdk/client"
)

// @@@SNIPSTART go-schedule-create
func CreateSchedule(ctx context.Context, temporalClient client.Client) (client.ScheduleHandle, error) {
	scheduleID := "schedule_id"
	workflowID := "schedule_workflow_id"

	scheduleHandle, err := temporalClient.ScheduleClient().Create(ctx, client.ScheduleOptions{
		ID:   scheduleID,
		Spec: client.ScheduleSpec{},
		Action: &client.ScheduleWorkflowAction{
			ID:        workflowID,
			Workflow:  ScheduleWorkflow,
			TaskQueue: TaskQueue,
		},
	})
	if err != nil {
		return nil, fmt.Errorf("unable to create schedule: %w", err)
	}
	log.Println("Schedule created", "ScheduleID", scheduleID)
	return scheduleHandle, nil
}
// @@@SNIPEND

// @@@SNIPSTART go-schedule-backfill
func BackfillSchedule(ctx context.Context, temporalClient client.Client) error {
	workflowID := "schedule_workflow_"
	// Create a paused Schedule to backfill
	now := time.Now()
	scheduleHandle, err := temporalClient.ScheduleClient().Create(ctx, client.ScheduleOptions{
		ID: "backfill-schedule",
		Spec: client.ScheduleSpec{
			Intervals: []client.ScheduleIntervalSpec{
				{
					Every: time.Minute,
				},
			},
		},
		Action: &client.ScheduleWorkflowAction{
			ID:        workflowID,
			Workflow:  ScheduleWorkflow,
			TaskQueue: TaskQueue,
		},
		Paused: true,
	})
	if err != nil {
		return fmt.Errorf("unable to create schedule: %w", err)
	}

	err = scheduleHandle.Backfill(ctx, client.ScheduleBackfillOptions{
		Backfill: []client.ScheduleBackfill{
			{
				Start:   now.Add(-4 * time.Minute),
				End:     now.Add(-2 * time.Minute),
				Overlap: enums.SCHEDULE_OVERLAP_POLICY_ALLOW_ALL,
			},
			{
				Start:   now.Add(-2 * time.Minute),
				End:     now,
				Overlap: enums.SCHEDULE_OVERLAP_POLICY_ALLOW_ALL,
			},
		},
	})
	if err != nil {
		return fmt.Errorf("unable to backfill schedule: %w", err)
	}
	return nil
}
// @@@SNIPEND

// @@@SNIPSTART go-schedule-delete
func DeleteSchedule(ctx context.Context, temporalClient client.Client) error {
	scheduleID := "schedule_id"
	// Retrieve the schedule handle by its ID
	scheduleHandle := temporalClient.ScheduleClient().GetHandle(ctx, scheduleID)

	log.Println("Deleting schedule", "ScheduleID", scheduleHandle.GetID())
	err := scheduleHandle.Delete(ctx)
	if err != nil {
		return fmt.Errorf("unable to delete schedule: %w", err)
	}
	return nil
}
// @@@SNIPEND

// @@@SNIPSTART go-schedule-describe
func DescribeSchedule(ctx context.Context, temporalClient client.Client) (*client.ScheduleDescription, error) {
	// create Schedule
	scheduleHandle, err := temporalClient.ScheduleClient().Create(ctx, client.ScheduleOptions{
		ID: "test-schedule-describe-spec-cron-schedule",
		Spec: client.ScheduleSpec{
			CronExpressions: []string{
				"0 12 * * MON",
			},
		},
		Action: &client.ScheduleWorkflowAction{
			ID:        "schedule_workflow_describe",
			Workflow:  ScheduleWorkflow,
			TaskQueue: TaskQueue,
		},
	})
	if err != nil {
		return nil, fmt.Errorf("unable to create schedule: %w", err)
	}
	// describe schedule
	description, err := scheduleHandle.Describe(ctx)
	if err != nil {
		return nil, fmt.Errorf("unable to describe schedule: %w", err)
	}
	log.Println("Schedule next action times", description.Info.NextActionTimes)
	return description, nil
}
// @@@SNIPEND

// @@@SNIPSTART go-schedule-list
func ListSchedules(ctx context.Context, temporalClient client.Client) error {
	// Create Schedule and Workflow IDs
	scheduleID := "schedule_" + uuid.NewString()
	workflowID := "schedule_workflow_" + uuid.NewString()
	// Create the schedule.
	scheduleHandle, err := temporalClient.ScheduleClient().Create(ctx, client.ScheduleOptions{
		ID:   scheduleID,
		Spec: client.ScheduleSpec{},
		Action: &client.ScheduleWorkflowAction{
			ID:        workflowID,
			Workflow:  ScheduleWorkflow,
			TaskQueue: TaskQueue,
		},
	})
	if err != nil {
		return fmt.Errorf("unable to create schedule: %w", err)
	}
	log.Println("Schedule created", "ScheduleID", scheduleHandle.GetID())
	// list schedules
	listView, err := temporalClient.ScheduleClient().List(ctx, client.ScheduleListOptions{
		PageSize: 1,
	})
	if err != nil {
		return fmt.Errorf("unable to list schedules: %w", err)
	}

	for listView.HasNext() {
		entry, err := listView.Next()
		if err != nil {
			return fmt.Errorf("unable to get next schedule: %w", err)
		}
		log.Println("Schedule", "ScheduleID", entry.ID)
	}
	return nil
}
// @@@SNIPEND

// @@@SNIPSTART go-schedule-pause
func PauseSchedule(ctx context.Context, temporalClient client.Client) error {
	scheduleID := "schedule_id"
	scheduleHandle := temporalClient.ScheduleClient().GetHandle(ctx, scheduleID)

	// Pause the schedule and print the status
	err := scheduleHandle.Pause(ctx, client.SchedulePauseOptions{
		Note: "The Schedule has been paused.",
	})
	if err != nil {
		return fmt.Errorf("unable to pause schedule: %w", err)
	}
	log.Println("The Schedule has been paused.")

	// Wait for 5 seconds
	time.Sleep(5 * time.Second)

	// Unpause the schedule
	err = scheduleHandle.Unpause(ctx, client.ScheduleUnpauseOptions{
		Note: "The Schedule has been unpaused.",
	})
	if err != nil {
		return fmt.Errorf("unable to unpause schedule: %w", err)
	}
	log.Println("The Schedule has been unpaused.")
	return nil
}
// @@@SNIPEND

// @@@SNIPSTART go-schedule-trigger
func TriggerSchedule(ctx context.Context, temporalClient client.Client) error {
	scheduleHandle, err := temporalClient.ScheduleClient().Create(ctx, client.ScheduleOptions{
		ID:   "trigger-schedule",
		Spec: client.ScheduleSpec{},
		Action: &client.ScheduleWorkflowAction{
			ID:        "trigger-schedule-workflow",
			Workflow:  ScheduleWorkflow,
			TaskQueue: TaskQueue,
		},
		Paused:  true,
		Overlap: enums.SCHEDULE_OVERLAP_POLICY_ALLOW_ALL,
	})
	if err != nil {
		return fmt.Errorf("unable to create schedule: %w", err)
	}

	// Trigger Schedule
	for i := 0; i < 5; i++ {
		err = scheduleHandle.Trigger(ctx, client.ScheduleTriggerOptions{
			Overlap: enums.SCHEDULE_OVERLAP_POLICY_ALLOW_ALL,
		})
		if err != nil {
			return fmt.Errorf("unable to trigger schedule: %w", err)
		}
		time.Sleep(2 * time.Second)
	}
	return nil
}
// @@@SNIPEND

// @@@SNIPSTART go-schedule-update
func UpdateSchedule(ctx context.Context, temporalClient client.Client) error {
	scheduleHandle, err := temporalClient.ScheduleClient().Create(ctx, client.ScheduleOptions{
		ID:   "update-schedule",
		Spec: client.ScheduleSpec{},
		Action: &client.ScheduleWorkflowAction{
			ID:        "update-schedule-workflow",
			Workflow:  ScheduleWorkflow,
			TaskQueue: TaskQueue,
		},
		Paused: true,
	})
	if err != nil {
		return fmt.Errorf("unable to create schedule: %w", err)
	}

	updateSchedule := func(input client.ScheduleUpdateInput) (*client.ScheduleUpdate, error) {
		// Change the Schedule to run every hour.
		input.Description.Schedule.Spec = &client.ScheduleSpec{
			Intervals: []client.ScheduleIntervalSpec{{Every: time.Hour}},
		}
		return &client.ScheduleUpdate{
			Schedule: &input.Description.Schedule,
		}, nil
	}

	err = scheduleHandle.Update(ctx, client.ScheduleUpdateOptions{
		DoUpdate: updateSchedule,
	})
	if err != nil {
		return fmt.Errorf("unable to update schedule: %w", err)
	}
	return nil
}
// @@@SNIPEND
