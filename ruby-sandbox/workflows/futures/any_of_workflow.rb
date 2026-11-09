# frozen_string_literal: true

require 'temporalio/error'
require 'temporalio/workflow'
require_relative 'activities'

# Returns the result of whichever of five Activities finishes first, or fails if a 30-second Timer fires first.
class AnyOfWorkflow < Temporalio::Workflow::Definition
  def execute
    # @@@SNIPSTART ruby-futures-any-of
    # Start 5 activities
    act_futs = 5.times.map do |i|
      Temporalio::Workflow::Future.new do
        Temporalio::Workflow.execute_activity(MyActivity, "my-arg-#{i}", schedule_to_close_timeout: 300)
      end
    end
    # Start a timer
    sleep_fut = Temporalio::Workflow::Future.new { Temporalio::Workflow.sleep(30) }

    # Wait for first act result or sleep fut
    act_result = Temporalio::Workflow::Future.any_of(sleep_fut, *act_futs).wait
    # Fail if timer done first
    raise Temporalio::Error::ApplicationError, 'Timer expired' if sleep_fut.done?
    # Print act result otherwise
    Temporalio::Workflow.logger.info("Act result: #{act_result}")
    # @@@SNIPEND
    act_result
  end
end
