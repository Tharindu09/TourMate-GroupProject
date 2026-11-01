import {api, setAccessToken} from '../api/client';
import type {AuthResponse, User} from '../models';
export async function authenticateUser(route: string, values: object) {
 const result = await api<AuthResponse>(`/auth/${route}`, {method: 'POST', body: JSON.stringify(values)});
 setAccessToken(result.accessToken);
 return result;
}
export function updateProfile(values: object) {
 return api<User>('/users/me', {method: 'PUT', body: JSON.stringify(values)});
}

export function clearSession() {setAccessToken(null);}

export function registerUser(values:object) {
 return api<AuthResponse>('/auth/register',{method:'POST',body:JSON.stringify(values)});
}
