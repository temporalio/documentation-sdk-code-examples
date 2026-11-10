# frozen_string_literal: true

# Each Activity file in this folder defines its own SomethingThatFails. Load one at a time.
require 'temporalio/activity'
require 'temporalio/error'

# @@@SNIPSTART ruby-error-handling-custom-error
class MyError < StandardError
end

class SomethingThatFails < Temporalio::Activity::Definition
  def execute(details)
    Temporalio::Activity::Context.current.logger.info(
      "We have a problem."
    )
    raise MyError.new('Simulated failure')
  end
end
# @@@SNIPEND
