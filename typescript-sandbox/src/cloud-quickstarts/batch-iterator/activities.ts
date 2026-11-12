import type { SingleRecord } from "./types";

/** The dataset is always 3 pages long. */
export const PAGE_COUNT = 3;

/**
 * Returns the next page of records, or an empty array when there are none left.
 *
 * Records are generated in memory so the sample needs no external storage. A real
 * application would page through a database table, a file, or an object store here.
 */
export async function getRecords(
  pageSize: number,
  offset: number,
): Promise<SingleRecord[]> {
  if (offset >= pageSize * PAGE_COUNT) {
    return [];
  }
  return Array.from({ length: pageSize }, (_, i) => ({ id: offset + i }));
}
