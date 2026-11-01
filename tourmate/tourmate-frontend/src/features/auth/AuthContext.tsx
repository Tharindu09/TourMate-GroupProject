import {createContext, useContext, useState, type ReactNode} from 'react';
import {useQueryClient} from '@tanstack/react-query';
import {api, setAccessToken} from '../../api/client';
import type {AuthResponse, User} from '../../types';
interface Auth {user: User | null; authenticate: (route: string, values: object) => Promise<void>; logout: () => void; updateUser: (user: User) => void}
const Context = createContext<Auth | null>(null);
export function AuthProvider({children}: {children: ReactNode}) {
  const [user, updateUser] = useState<User | null>(null);
  const client = useQueryClient();
  async function authenticate(route: string, values: object) {
    const result = await api<AuthResponse>(`/auth/${route}`, {method: 'POST', body: JSON.stringify(values)});
    setAccessToken(result.accessToken); updateUser(result.user);
  }
  function logout() {setAccessToken(null); updateUser(null); client.clear();}
  return <Context.Provider value={{user, authenticate, logout, updateUser}}>{children}</Context.Provider>;
}
export function useAuth() {const auth = useContext(Context); if (!auth) throw new Error('AuthProvider required'); return auth;}
