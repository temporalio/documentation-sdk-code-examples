package batchiterator

import "context"

// PageCount is the number of pages in the dataset.
const PageCount = 3

// GetRecords returns the next page of records, or an empty slice when there are
// none left.
//
// Records are generated in memory so the sample needs no external storage. A
// real application would page through a database table, a file, or an object
// store here.
func GetRecords(_ context.Context, pageSize, offset int) ([]SingleRecord, error) {
	records := make([]SingleRecord, 0, pageSize)
	if offset < pageSize*PageCount {
		for i := 0; i < pageSize; i++ {
			records = append(records, SingleRecord{ID: offset + i})
		}
	}
	return records, nil
}
