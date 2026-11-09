# frozen_string_literal: true

require 'temporalio/client'

def cancel_workflow(my_client)
  # @@@SNIPSTART ruby-cancellation-cancel-workflow
  # Get a workflow handle by its workflow ID. This could be made specific to a run by
  # passing run ID. This could also just be a handle that is returned from
  # start_workflow instead.
  handle = my_client.workflow_handle('my-workflow-id')

  # Send cancellation. This returns when cancellation is received by the server. Wait on
  # the handle's result to wait for cancellation to be applied.
  handle.cancel
  # @@@SNIPEND
end

def terminate_workflow(my_client)
  # @@@SNIPSTART ruby-cancellation-terminate-workflow
  # Get a workflow handle by its workflow ID. This could be made specific to a run by
  # passing run ID. This could also just be a handle that is returned from
  # start_workflow instead.
  handle = my_client.workflow_handle('my-workflow-id')

  # Terminate
  handle.terminate
  # @@@SNIPEND
end
