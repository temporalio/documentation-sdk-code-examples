# frozen_string_literal: true

require 'temporalio/activity'

module BatchIterator
  # Returns the next page of records, or an empty array when there are none left.
  #
  # Each record is a Hash such as { 'id' => 7 }, which the default JSON converter
  # round-trips as-is. Records are generated in memory so the sample needs no
  # external storage. A real application would page through a database table, a
  # file, or an object store here.
  class GetRecordsActivity < Temporalio::Activity::Definition
    # The dataset is always 3 pages long.
    PAGE_COUNT = 3

    def execute(page_size, offset)
      return [] if offset >= page_size * PAGE_COUNT

      Array.new(page_size) { |i| { 'id' => offset + i } }
    end
  end
end
