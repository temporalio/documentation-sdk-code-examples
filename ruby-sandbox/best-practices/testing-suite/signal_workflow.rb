# frozen_string_literal: true

require 'temporalio/workflow'

# @@@SNIPSTART ruby-testing-signal-workflow
class SignalWorkflow < Temporalio::Workflow::Definition
  def execute
    # Wait for signal or timeout in 45 seconds
    Temporalio::Workflow.timeout(45) do
      Temporalio::Workflow.wait_condition { @signal_received }
    end
    'got signal'
  rescue Timeout::Error
    'got timeout'
  end

  workflow_signal
  def some_signal
    @signal_received = true
  end
end
# @@@SNIPEND
