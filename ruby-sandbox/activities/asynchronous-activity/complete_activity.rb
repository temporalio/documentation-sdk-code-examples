# frozen_string_literal: true

require 'temporalio/client'

# Run by the external system that received the Task Token from MyAsyncActivity.
def complete_activity(my_client, captured_token)
  # @@@SNIPSTART ruby-async-activity-get-handle
  handle = my_client.async_activity_handle(captured_token)
  # @@@SNIPEND

  # @@@SNIPSTART ruby-async-activity-complete
  handle.complete('completion value')
  # @@@SNIPEND
end
