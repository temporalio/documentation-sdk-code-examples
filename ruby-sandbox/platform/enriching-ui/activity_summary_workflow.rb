# frozen_string_literal: true

# Each Workflow file in this folder defines its own YourWorkflow. Load one at a time.

# @@@SNIPSTART ruby-enriching-ui-activity-summary
require 'temporalio/workflow'

class YourWorkflow < Temporalio::Workflow::Definition
  def execute(input)
    # Execute an activity with a summary
    result = Temporalio::Workflow.execute_activity(
      'YourActivity',
      input,
      start_to_close_timeout: 10,
      summary: 'Processing user data'
    )

    result
  end
end
# @@@SNIPEND
