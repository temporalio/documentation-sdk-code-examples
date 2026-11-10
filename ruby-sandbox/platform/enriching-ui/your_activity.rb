# frozen_string_literal: true

require 'temporalio/activity'

# The Activity that activity_summary_workflow.rb calls by name.
class YourActivity < Temporalio::Activity::Definition
  def execute(input)
    "processed #{input}"
  end
end
