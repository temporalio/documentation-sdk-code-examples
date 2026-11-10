# frozen_string_literal: true

# Each Workflow file in this folder defines its own YourWorkflow. Load one at a time.

# @@@SNIPSTART ruby-enriching-ui-current-details
require 'temporalio/workflow'

class YourWorkflow < Temporalio::Workflow::Definition
  def execute(input)
    # Get the current details
    current_details = Temporalio::Workflow.current_details
    Temporalio::Workflow.logger.info("Current details: #{current_details}")

    # Set/update the current details
    Temporalio::Workflow.current_details = 'Updated workflow details with new status'

    'Workflow completed'
  end
end
# @@@SNIPEND
