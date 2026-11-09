# frozen_string_literal: true

require 'temporalio/activity'
require 'temporalio/workflow'

class CheckTitleValidityActivity < Temporalio::Activity::Definition
  def execute(name_with_title)
    name_with_title.start_with?('Sir ')
  end
end

# @@@SNIPSTART ruby-message-passing-workflow-init
class WorkflowInitWorkflow < Temporalio::Workflow::Definition
  workflow_init
  def initialize(input)
    @name_with_title = "Sir #{input['name']}"
  end

  def execute(input)
    Temporalio::Workflow.wait_condition { @title_has_been_checked }
    "Hello, #{@name_with_title}"
  end

  workflow_update
  def check_title_validity
    # The handler is now guaranteed to see some workflow input since it was
    # processed by the constructor
    valid = Temporalio::Workflow.execute_activity(
      CheckTitleValidityActivity,
      @name_with_title,
      start_to_close_timeout: 100
    )
    @title_has_been_checked = true
    valid
  end
end
# @@@SNIPEND
