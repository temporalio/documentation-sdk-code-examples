# frozen_string_literal: true

require 'temporalio/activity'
require 'temporalio/error'

# Fails its first attempt with an error that sets the delay before the next
# attempt, then succeeds.
class NextRetryDelayActivity < Temporalio::Activity::Definition
  def execute
    return 'done' if Temporalio::Activity::Context.current.info.attempt > 1

    # @@@SNIPSTART ruby-activity-timeouts-next-retry-delay
    raise Temporalio::Error::ApplicationError.new(
      'Some error',
      type: 'SomeErrorType',
      next_retry_delay: 3 * Temporalio::Activity::Context.current.info.attempt
    )
    # @@@SNIPEND
  end
end
