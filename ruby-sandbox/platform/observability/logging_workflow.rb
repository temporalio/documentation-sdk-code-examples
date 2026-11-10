# frozen_string_literal: true

require 'temporalio/activity'
require 'temporalio/workflow'

class LoggingActivity < Temporalio::Activity::Definition
  def execute(some_value)
    # @@@SNIPSTART ruby-observability-activity-logger
    Temporalio::Activity::Context.current.logger.info("Some log #{some_value}")
    # @@@SNIPEND
  end
end

class LoggingWorkflow < Temporalio::Workflow::Definition
  def execute(some_value)
    # @@@SNIPSTART ruby-observability-workflow-logger
    Temporalio::Workflow.logger.info("Some log #{some_value}")
    # @@@SNIPEND
    Temporalio::Workflow.execute_activity(LoggingActivity, some_value, start_to_close_timeout: 10)
  end
end
