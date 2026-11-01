import {useState, type FormEvent} from 'react';
import {Link, Navigate, useLocation, useNavigate} from 'react-router-dom';
import {useAuth} from '../features/auth/AuthContext';
import {registerUser, updateProfile} from '../services/authService';
export function AuthPage({register = false}: {register?: boolean}) {
 const location=useLocation();const registered=location.state as {registered?:boolean;email?:string}|null;
 const {user, authenticate} = useAuth(); const navigate = useNavigate(); const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
 if (user) return <Navigate to="/" replace/>;
 async function submit(e: FormEvent<HTMLFormElement>) {
  e.preventDefault(); setError(''); setBusy(true);
  const values = Object.fromEntries(new FormData(e.currentTarget));
  if(register&&values.password!==values.confirmPassword){setError('Passwords do not match.');setBusy(false);return;}
  if(new TextEncoder().encode(String(values.password)).length>72){setError('Password must be at most 72 UTF-8 bytes.');setBusy(false);return;}
  const credentials={email:String(values.email).trim(),password:values.password};
  try {if(register){await registerUser({...credentials,firstName:String(values.firstName).trim(),lastName:String(values.lastName).trim()});navigate('/login',{replace:true,state:{registered:true,email:credentials.email}});}else{await authenticate('login',credentials);navigate('/',{replace:true});}} catch (e) {setError((e as Error).message);} finally {setBusy(false);}
 }
 return <section className="auth-page"><div className="eyebrow">YOUR NEXT CHAPTER STARTS HERE</div><h1>{register ? 'Hello, explorer.' : 'Welcome back.'}</h1><p className="muted">{register ? 'Make yourself at home with TourMate.' : 'A whole island is waiting for you.'}</p><form onSubmit={submit} className="auth-form">{!register&&registered?.registered&&<p role="status">Account created. Sign in with your email and password.</p>}{register && <><label>First name<input required name="firstName" maxLength={80} autoComplete="given-name"/></label><label>Last name<input required name="lastName" maxLength={80} autoComplete="family-name"/></label></>}<label>Email<input required name="email" type="email" maxLength={254} autoComplete="email" defaultValue={!register?registered?.email:undefined}/></label><label>Password<input required name="password" type="password" minLength={register ? 8 : 1} maxLength={72} autoComplete={register ? 'new-password' : 'current-password'}/></label>{register&&<label>Confirm password<input required name="confirmPassword" type="password" minLength={8} maxLength={72} autoComplete="new-password"/></label>}{register && <small>Use at least 8 characters (up to 72 UTF-8 bytes).</small>}{error && <p className="error" role="alert">{error}</p>}<button className="primary" disabled={busy}>{busy ? 'Please wait…' : register ? 'Create account →' : 'Sign in →'}</button></form><p>{register ? 'Already exploring? ' : 'New around here? '}<Link className="text-link" to={register ? '/login' : '/register'}>{register ? 'Sign in' : 'Create an account'}</Link></p></section>;
}
export function Profile() {
 const {user, updateUser, logout} = useAuth();const [signingOut,setSigningOut]=useState(false); const [message, setMessage] = useState(''); const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
 if (!user) return <Navigate to={signingOut?'/':'/login'} replace/>;
 async function save(e: FormEvent<HTMLFormElement>) {
  e.preventDefault(); setBusy(true); setError(''); setMessage('');
  try {const updated = await updateProfile(Object.fromEntries(new FormData(e.currentTarget))); updateUser(updated); setMessage('Profile saved.');} catch(e) {setError((e as Error).message);} finally {setBusy(false);}
 }
 return <section className="auth-page"><p className="eyebrow">YOUR TOURMATE ACCOUNT</p><h1>Hi, {user.firstName}.</h1><p className="muted">{user.email}</p><form className="auth-form" onSubmit={save}><label>First name<input required name="firstName" defaultValue={user.firstName} maxLength={80}/></label><label>Last name<input required name="lastName" defaultValue={user.lastName} maxLength={80}/></label><button className="primary" disabled={busy}>{busy ? 'Saving…' : 'Save profile'}</button></form>{error && <p role="alert" className="error">{error}</p>}{message && <p role="status">{message}</p>}<button className="signout" onClick={()=>{setSigningOut(true);logout();}}>Sign out</button></section>;
}
