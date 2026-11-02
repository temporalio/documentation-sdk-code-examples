import http from 'http';

export async function makeHTTPRequest(url: string): Promise<string> {
  return new Promise((resolve, reject) => {
    http.get(url, (res) => resolve(String(res.statusCode))).on('error', reject);
  });
}
