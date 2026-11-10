# frozen_string_literal: true

require 'temporalio/workflow'

# @@@SNIPSTART ruby-testing-wait-a-day-workflow
class WaitADayWorkflow < Temporalio::Workflow::Definition
  def execute
    Temporalio::Workflow.sleep(1 * 24 * 60 * 60)
    'all done'
  end
end
# @@@SNIPEND
