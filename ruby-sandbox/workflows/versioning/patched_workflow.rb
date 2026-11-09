# frozen_string_literal: true

require 'temporalio/workflow'
require_relative 'activities'

# Each *_workflow.rb file in this folder is one revision of the same MyWorkflow, so load only one of them at a time.
# @@@SNIPSTART ruby-versioning-patched
class MyWorkflow < Temporalio::Workflow::Definition
  def execute
    if Temporalio::Workflow.patched('my-patch')
      result = Temporalio::Workflow.execute_activity(
        PostPatchActivity,
        start_to_close_timeout: 100
      )
    else
      result = Temporalio::Workflow.execute_activity(
        PrePatchActivity,
        start_to_close_timeout: 100
      )
    end

    # ...
    result
  end
end
# @@@SNIPEND
