let accessToken: string | null = null;
export function setAccessToken(token: string | null) { accessToken = token; }
export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body) headers.set('Content-Type', 'application/json');
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);
  const response = await fetch(`/api${path}`, {...options, headers});
  if (!response.ok) {
    const problem = await response.json().catch(() => null);
    const fields = problem?.errors ? Object.entries(problem.errors).map(([key, value]) => `${key}: ${value}`).join('; ') : '';
    throw new Error(fields || problem?.detail || (response.status === 401 ? 'Please sign in again.' : 'Unable to complete the request. Please try again.'));
  }
  return response.json() as Promise<T>;
}
