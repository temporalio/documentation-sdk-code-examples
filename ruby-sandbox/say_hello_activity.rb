require 'temporalio/activity'

# Implementation of a simple activity
class SayHelloActivity < Temporalio::Activity::Definition
  def execute(name)
    "Hello, #{name}!"
  end
end