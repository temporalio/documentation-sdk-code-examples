# frozen_string_literal: true

require 'temporalio/activity'

# The module keeps this MyActivity apart from the one in ../basics.
module Heartbeat
  # @@@SNIPSTART ruby-activity-timeouts-heartbeat
  class MyActivity < Temporalio::Activity::Definition
    def execute
      # This is a naive loop simulating work, but similar heartbeat logic
      # applies to other scenarios as well
      loop do
        # Send heartbeat
        Temporalio::Activity::Context.current.heartbeat
        # Sleep before heartbeating again
        sleep(3)
      end
    end
  end
  # @@@SNIPEND
end
