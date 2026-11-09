# frozen_string_literal: true

require 'temporalio/activity'

class MyCleanupActivity < Temporalio::Activity::Definition
  def execute
    Temporalio::Activity::Context.current.logger.info('Cleaning up after cancellation')
  end
end
