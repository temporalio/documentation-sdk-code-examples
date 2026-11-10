# frozen_string_literal: true

# Each Activity file in this folder defines its own SomethingThatFails. Load one at a time.
require 'temporalio/activity'
require 'temporalio/error'

# @@@SNIPSTART ruby-error-handling-application-error
class SomethingThatFails < Temporalio::Activity::Definition
  def execute(details)
    Temporalio::Activity::Context.current.logger.info(
      "We have a problem."
    )
    raise Temporalio::Error::ApplicationError.new('Simulated failure', type: 'MyError')
  end
end
# @@@SNIPEND
