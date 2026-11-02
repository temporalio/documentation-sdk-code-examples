// Returns a summary of a document that may be larger than the payload size threshold
export async function processDocument(document: string): Promise<string> {
  return `Processed ${document.length} characters`;
}

// Returns a payload large enough to be offloaded to External Storage
export async function generateReport(sizeInBytes: number): Promise<string> {
  return 'x'.repeat(sizeInBytes);
}
