# frozen_string_literal: true

require 'temporalio/activity'

# @@@SNIPSTART ruby-activity-basics-define
class MyActivity < Temporalio::Activity::Definition
  def execute(input)
    "#{input['greeting']}, #{input['name']}!"
  end
end
# @@@SNIPEND
