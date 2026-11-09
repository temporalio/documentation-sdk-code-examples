# frozen_string_literal: true

require 'temporalio/retry_policy'
require 'temporalio/workflow'
require_relative '../basics/my_activity'

class StartToCloseWorkflow < Temporalio::Workflow::Definition
  def execute(name)
    # @@@SNIPSTART ruby-activity-timeouts-start-to-close
    Temporalio::Workflow.execute_activity(
      MyActivity,
      { greeting: 'Hello', name: },
      start_to_close_timeout: 5 * 60
    )
    # @@@SNIPEND
  end
end

class RetryPolicyWorkflow < Temporalio::Workflow::Definition
  def execute(name)
    # @@@SNIPSTART ruby-activity-timeouts-retry-policy
    Temporalio::Workflow.execute_activity(
      MyActivity,
      { greeting: 'Hello', name: },
      start_to_close_timeout: 5 * 60,
      retry_policy: Temporalio::RetryPolicy.new(max_interval: 10)
    )
    # @@@SNIPEND
  end
end

class HeartbeatTimeoutWorkflow < Temporalio::Workflow::Definition
  def execute(name)
    # @@@SNIPSTART ruby-activity-timeouts-heartbeat-timeout
    Temporalio::Workflow.execute_activity(
      MyActivity,
      { greeting: 'Hello', name: },
      start_to_close_timeout: 5 * 60,
      heartbeat_timeout: 5
    )
    # @@@SNIPEND
  end
end
