# frozen_string_literal: true

require 'temporalio/activity'

class SayHello < Temporalio::Activity::Definition
  def execute(name)
    "Hello, #{name}!"
  end
end
