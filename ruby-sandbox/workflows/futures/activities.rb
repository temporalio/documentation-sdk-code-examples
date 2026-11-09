# frozen_string_literal: true

require 'temporalio/activity'

class MyActivity1 < Temporalio::Activity::Definition
  def execute
    'one'
  end
end

class MyActivity2 < Temporalio::Activity::Definition
  def execute
    'two'
  end
end

class MyActivity3 < Temporalio::Activity::Definition
  def execute
    'three'
  end
end

class MyActivity < Temporalio::Activity::Definition
  def execute(arg)
    "result for #{arg}"
  end
end
