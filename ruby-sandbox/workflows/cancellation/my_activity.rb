# frozen_string_literal: true

require 'temporalio/activity'

# @@@SNIPSTART ruby-cancellation-activity
class MyActivity < Temporalio::Activity::Definition
  def execute
    # This is a naive loop simulating work, but similar heartbeat/cancellation logic
    # applies to other scenarios as well
    loop do
      # Send heartbeat
      Temporalio::Activity::Context.current.heartbeat
      # Sleep before heartbeating again
      sleep(3)
    end
  rescue Temporalio::Error::CanceledError
    raise 'Canceled!'
  end
end
# @@@SNIPEND
