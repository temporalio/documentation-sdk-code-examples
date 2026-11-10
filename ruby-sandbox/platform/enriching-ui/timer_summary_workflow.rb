# frozen_string_literal: true

# Each Workflow file in this folder defines its own YourWorkflow. Load one at a time.

# @@@SNIPSTART ruby-enriching-ui-timer-summary
require 'temporalio/workflow'

class YourWorkflow < Temporalio::Workflow::Definition
  def execute(input)
    # Create a timer with a summary
    Temporalio::Workflow.sleep(300, summary: 'Waiting for payment confirmation')

    'Timer completed'
  end
end
# @@@SNIPEND
