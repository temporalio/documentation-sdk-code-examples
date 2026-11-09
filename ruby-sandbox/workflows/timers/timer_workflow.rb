# frozen_string_literal: true

require 'temporalio/workflow'

class TimerWorkflow < Temporalio::Workflow::Definition
  def execute
    # @@@SNIPSTART ruby-timers-sleep
    # Sleep for 72 hours
    Temporalio::Workflow.sleep(72 * 60 * 60, summary: 'my timer')
    # @@@SNIPEND
    'Timer fired'
  end
end
