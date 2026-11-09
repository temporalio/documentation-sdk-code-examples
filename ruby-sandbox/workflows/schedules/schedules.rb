# frozen_string_literal: true

require 'temporalio/client'
require_relative 'my_workflow'

def create_schedule(my_client)
  # @@@SNIPSTART ruby-schedules-create
  handle = my_client.create_schedule(
    'my-schedule-id',
    Temporalio::Client::Schedule.new(
      action: Temporalio::Client::Schedule::Action::StartWorkflow.new(
        MyWorkflow, 'some-input',
        id: 'my-workflow-id', task_queue: 'my-task-queue'
      ),
      spec: Temporalio::Client::Schedule::Spec.new(
        intervals: [
          Temporalio::Client::Schedule::Spec::Interval.new(
            every: 5 * 24 * 60 * 60.0, # 5 days
          )
        ]
      )
    )
  )
  # @@@SNIPEND
  handle
end

def backfill_schedule(my_client)
  # @@@SNIPSTART ruby-schedules-backfill
  handle = my_client.schedule_handle('my-schedule-id')
  now = Time.now(in: 'UTC')
  handle.backfill(
    Temporalio::Client::Schedule::Backfill.new(
      start_at: now - (4 * 60),
      end_at: now - (2 * 60),
      overlap: Temporalio::Client::Schedule::OverlapPolicy::ALLOW_ALL
    )
  )
  # @@@SNIPEND
end

def delete_schedule(my_client)
  # @@@SNIPSTART ruby-schedules-delete
  handle = my_client.schedule_handle('my-schedule-id')
  handle.delete
  # @@@SNIPEND
end

def describe_schedule(my_client)
  # @@@SNIPSTART ruby-schedules-describe
  handle = my_client.schedule_handle('my-schedule-id')
  desc = handle.describe
  puts "Schedule info: #{desc.info}"
  # @@@SNIPEND
  desc
end

def list_schedules(my_client)
  # @@@SNIPSTART ruby-schedules-list
  my_client.list_schedules.each do |sched|
    puts "Schedule info: #{sched}"
  end
  # @@@SNIPEND
end

def pause_schedule(my_client)
  # @@@SNIPSTART ruby-schedules-pause
  handle = my_client.schedule_handle('my-schedule-id')
  handle.pause(note: 'Pausing the schedule for now')
  # @@@SNIPEND
end

def trigger_schedule(my_client)
  # @@@SNIPSTART ruby-schedules-trigger
  handle = my_client.schedule_handle('my-schedule-id')
  handle.trigger
  # @@@SNIPEND
end

def update_schedule(my_client)
  # @@@SNIPSTART ruby-schedules-update
  handle = my_client.schedule_handle('my-schedule-id')
  handle.update do |input|
    # Return a new schedule with the action updated
    Temporalio::Client::Schedule::Update.new(
      schedule: input.description.schedule.with(
        # Update the action
        action: Temporalio::Client::Schedule::Action::StartWorkflow.new(
          MyNewWorkflow, 'some-new-input',
          id: 'my-workflow-id', task_queue: 'my-task-queue'
        )
      )
    )
  end
  # @@@SNIPEND
end

def start_with_delay(my_client)
  # @@@SNIPSTART ruby-schedules-start-delay
  handle = my_client.start_workflow(
    MyWorkflow, 'some-input',
    id: 'my-workflow-id', task_queue: 'my-task-queue',
    start_delay: 3 * 60 * 60 # 3 hours
  )
  # @@@SNIPEND
  handle
end
