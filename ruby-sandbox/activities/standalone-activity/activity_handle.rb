# frozen_string_literal: true

require 'temporalio/client'

# Examples of working with a handle to a Standalone Activity that was already
# started, for example by start_activity.rb.

def get_activity_handle(client)
  # @@@SNIPSTART ruby-standalone-activity-get-handle
  handle = client.activity_handle('standalone-activity-id')
  # @@@SNIPEND
  handle
end

def wait_for_result(handle)
  # @@@SNIPSTART ruby-standalone-activity-result
  result = handle.result
  # @@@SNIPEND
  result
end

# Shows each operation on its own. A closed Activity can't be canceled or terminated.
def use_activity_handle(handle)
  # @@@SNIPSTART ruby-standalone-activity-handle-operations
  handle.result      # block until the activity completes; returns the result
  handle.describe    # fetch metadata (status, timestamps, attempt, last failure, etc.)
  handle.cancel      # request cancellation
  handle.terminate   # force-close the activity
  # @@@SNIPEND
end
