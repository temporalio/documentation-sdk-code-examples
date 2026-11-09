# frozen_string_literal: true

require 'temporalio/activity'

class PrePatchActivity < Temporalio::Activity::Definition
  def execute
    'pre-patch'
  end
end

class PostPatchActivity < Temporalio::Activity::Definition
  def execute
    'post-patch'
  end
end
