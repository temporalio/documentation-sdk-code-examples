# frozen_string_literal: true

require 'temporalio/cancellation'
require 'temporalio/workflow'
require_relative 'my_activity'
require_relative 'my_cleanup_activity'

module HandleCancellation
  # @@@SNIPSTART ruby-cancellation-handle-in-workflow
  class MyWorkflow < Temporalio::Workflow::Definition
    def execute
      # Whether this workflow waits on the activity to handle the cancellation or not is
      # dependent upon the cancellation_type parameter. We leave the default here which
      # sends the cancellation but does not wait on it to be handled.
      Temporalio::Workflow.execute_activity(MyActivity, start_to_close_timeout: 100)
    rescue Temporalio::Error => e
      # For this sample, we only want to execute cleanup when it's a cancellation
      raise unless Temporalio::Error.canceled?(e)

      # Call a cleanup activity. We have to do this with a new/detached cancellation
      # because the default workflow-level one is already canceled at this point.
      Temporalio::Workflow.execute_activity(
        MyCleanupActivity,
        start_to_close_timeout: 100,
        cancellation: Temporalio::Cancellation.new
      )

      # Re-raise the original exception
      raise
    end
  end
  # @@@SNIPEND
end
