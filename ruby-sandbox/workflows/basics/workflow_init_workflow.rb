# frozen_string_literal: true

require 'temporalio/workflow'

# @@@SNIPSTART ruby-workflow-basics-workflow-init
class WorkflowInitWorkflow < Temporalio::Workflow::Definition
  workflow_init
  def initialize(input)
    @name_with_title = "Knight #{input['name']}"
    @title_has_been_checked = false
  end

  def execute(input)
    Temporalio::Workflow.wait_condition { @title_has_been_checked }
    "Hello, #{@name_with_title}"
  end

  # The constructor has already run, so this handler can use @name_with_title
  # even if it runs before execute.
  workflow_update
  def check_title
    @title_has_been_checked = true
    @name_with_title
  end
end
# @@@SNIPEND
