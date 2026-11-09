# frozen_string_literal: true

require 'temporalio/activity'

class MyActivity < Temporalio::Activity::Definition
  def execute(input)
    "#{input['greeting']}, #{input['name']}!"
  end
end
