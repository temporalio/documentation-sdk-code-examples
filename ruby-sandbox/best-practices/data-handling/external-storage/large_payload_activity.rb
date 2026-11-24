# frozen_string_literal: true

require 'temporalio/activity'

class LargePayloadActivity < Temporalio::Activity::Definition
  def execute(input)
    input
  end
end
