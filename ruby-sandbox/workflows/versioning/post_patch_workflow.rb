# frozen_string_literal: true

require 'temporalio/workflow'
require_relative 'activities'

# Each *_workflow.rb file in this folder is one revision of the same MyWorkflow, so load only one of them at a time.
# @@@SNIPSTART ruby-versioning-post-patch
class MyWorkflow < Temporalio::Workflow::Definition
  def execute
    result = Temporalio::Workflow.execute_activity(
      PostPatchActivity,
      start_to_close_timeout: 100
    )

    # ...
    result
  end
end
# @@@SNIPEND
