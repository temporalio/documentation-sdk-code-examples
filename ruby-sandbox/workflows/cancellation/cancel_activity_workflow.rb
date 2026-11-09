# frozen_string_literal: true

require 'temporalio/cancellation'
require 'temporalio/workflow'
require_relative 'my_activity'

module CancelActivity
  # @@@SNIPSTART ruby-cancellation-cancel-activity
  class MyWorkflow < Temporalio::Workflow::Definition
    def execute
      # Create a new cancellation linked to the workflow one, so that it inherits
      # cancellation that comes from the workflow. Users can choose to make it
      # completely detached by not providing a parent.
      cancellation, cancel_proc = Temporalio::Cancellation.new(
        Temporalio::Workflow.cancellation
      )

      # Start the activity in the background. Whether this workflow waits on the activity
      # to handle the cancellation or not is dependent upon the cancellation_type
      # parameter. We leave the default here which sends the cancellation but does not wait
      # on it to be handled.
      future = Temporalio::Workflow::Future.new do
        Temporalio::Workflow.execute_activity(
          MyActivity,
          start_to_close_timeout: 100,
          cancellation:
        )
      end

      # Wait 5 minutes, then cancel it
      Temporalio::Workflow.sleep(5 * 60)
      cancel_proc.call

      # Wait on the activity which will raise an activity error with a cause of
      # cancellation which will fail the workflow
      future.wait
    end
  end
  # @@@SNIPEND
end
