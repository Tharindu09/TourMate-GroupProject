import {useState, type FormEvent} from 'react';
import {Link, Navigate, useNavigate} from 'react-router-dom';
import {useAuth} from '../features/auth/AuthContext';
import {api} from '../api/client';
import type {User} from '../types';
export function AuthPage({register = false}: {register?: boolean}) {
 const {user, authenticate} = useAuth(); const navigate = useNavigate(); const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
 if (user) return <Navigate to="/profile" replace/>;
 async function submit(e: FormEvent<HTMLFormElement>) {
  e.preventDefault(); setError(''); setBusy(true);
  const values = Object.fromEntries(new FormData(e.currentTarget));
  try {await authenticate(register ? 'register' : 'login', values); navigate('/profile');} catch (e) {setError((e as Error).message);} finally {setBusy(false);}
 }
 return <section className="auth-page"><div className="eyebrow">YOUR NEXT CHAPTER STARTS HERE</div><h1>{register ? 'Hello, explorer.' : 'Welcome back.'}</h1><p className="muted">{register ? 'Make yourself at home with TourMate.' : 'A whole island is waiting for you.'}</p><form onSubmit={submit} className="auth-form">{register && <><label>First name<input required name="firstName" maxLength={80} autoComplete="given-name"/></label><label>Last name<input required name="lastName" maxLength={80} autoComplete="family-name"/></label></>}<label>Email<input required name="email" type="email" maxLength={254} autoComplete="email"/></label><label>Password<input required name="password" type="password" minLength={register ? 8 : 1} maxLength={72} autoComplete={register ? 'new-password' : 'current-password'}/></label>{register && <small>Use at least 8 characters (up to 72 UTF-8 bytes).</small>}{error && <p className="error" role="alert">{error}</p>}<button className="primary" disabled={busy}>{busy ? 'Please wait…' : register ? 'Create account →' : 'Sign in →'}</button></form><p>{register ? 'Already exploring? ' : 'New around here? '}<Link className="text-link" to={register ? '/login' : '/register'}>{register ? 'Sign in' : 'Create an account'}</Link></p></section>;
}
export function Profile() {
 const {user, updateUser, logout} = useAuth(); const [message, setMessage] = useState(''); const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
 if (!user) return <Navigate to="/login" replace/>;
 async function save(e: FormEvent<HTMLFormElement>) {
  e.preventDefault(); setBusy(true); setError(''); setMessage('');
  try {const updated = await api<User>('/users/me', {method: 'PUT', body: JSON.stringify(Object.fromEntries(new FormData(e.currentTarget)))}); updateUser(updated); setMessage('Profile saved.');} catch(e) {setError((e as Error).message);} finally {setBusy(false);}
 }
 return <section className="auth-page"><p className="eyebrow">YOUR TOURMATE ACCOUNT</p><h1>Hi, {user.firstName}.</h1><p className="muted">{user.email}</p><form className="auth-form" onSubmit={save}><label>First name<input required name="firstName" defaultValue={user.firstName} maxLength={80}/></label><label>Last name<input required name="lastName" defaultValue={user.lastName} maxLength={80}/></label><button className="primary" disabled={busy}>{busy ? 'Saving…' : 'Save profile'}</button></form>{error && <p role="alert" className="error">{error}</p>}{message && <p role="status">{message}</p>}<button className="signout" onClick={logout}>Sign out</button></section>;
}
