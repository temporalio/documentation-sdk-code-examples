# frozen_string_literal: true

# @@@SNIPSTART ruby-benign-exceptions
require 'temporalio/activity'

class MyActivity < Temporalio::Activity::Definition
  def execute
    begin
      call_external_service
    rescue StandardError => e
      # Mark this error as benign since it's expected
      raise Temporalio::Error::ApplicationError.new(
        e.message,
        category: Temporalio::Error::ApplicationError::Category::BENIGN
      )
    end
  end
end
# @@@SNIPEND

# Stands in for a call to an external service that isn't ready yet.
class MyActivity
  private

  def call_external_service
    raise 'Service not ready'
  end
end
