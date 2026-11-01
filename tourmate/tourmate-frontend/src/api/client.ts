let accessToken: string | null = null;
export function setAccessToken(token: string | null) { accessToken = token; }
export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body) headers.set('Content-Type', 'application/json');
  if (accessToken && path !== '/auth/login' && path !== '/auth/register') headers.set('Authorization', `Bearer ${accessToken}`);
  let response:Response;
  try {response = await fetch(`/api${path}`, {...options, headers});}
  catch(error){if(error instanceof Error&&error.name==='AbortError')throw error;throw new Error('Cannot connect to TourMate. Check your connection and that the frontend and backend are running.');}
  if (!response.ok) {
    const problem = await response.json().catch(() => null);
    const fields = problem?.errors ? Object.entries(problem.errors).map(([key, value]) => `${key}: ${value}`).join('; ') : '';
    const fallback=response.status===401?(path==='/auth/login'?'Invalid email or password.':'Please sign in again.'):response.status===403?'The request was refused (403). Check the backend access and allowed website address.':[502,503,504].includes(response.status)?`TourMate backend is unavailable (${response.status}). Check the backend container and retry.`:response.status>=500?`TourMate encountered a server error (${response.status}). Check the backend logs and retry.`:'Unable to complete the request. Please try again.';
    throw new Error(fields || problem?.detail || fallback);
  }
  return response.json() as Promise<T>;
}
