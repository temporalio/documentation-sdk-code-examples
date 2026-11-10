# frozen_string_literal: true

require 'temporalio/activity'

# Hands its Task Token to an external system and returns without completing.
# The external system completes the Activity later with the Temporal Client.
class MyAsyncActivity < Temporalio::Activity::Definition
  # external_system is anything that accepts the token with <<, such as a Queue.
  def initialize(external_system)
    @external_system = external_system
  end

  def execute
    # @@@SNIPSTART ruby-async-activity-complete-async
    # Capture token for later completion
    captured_token = Temporalio::Activity::Context.current.info.task_token
    # Give the token to the external system that will complete the Activity
    @external_system << captured_token

    # Raise a special exception that says an activity will be completed somewhere else
    raise Temporalio::Activity::CompleteAsyncError
    # @@@SNIPEND
  end
end
