// @vitest-environment jsdom
import '@testing-library/jest-dom/vitest';
import {afterEach,describe,expect,it,vi} from 'vitest';
import {cleanup,fireEvent,render,screen,waitFor} from '@testing-library/react';
import {QueryClient,QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter,Route,Routes,Link} from 'react-router-dom';
import {AuthProvider,useAuth} from '../features/auth/AuthContext';
import {AuthPage,Profile} from './Auth';
import {api,setAccessToken} from '../api/client';
const user={id:1,firstName:'Test',lastName:'Traveller',email:'test@example.com',profileImageUrl:null,role:'USER'};
const response={user,accessToken:'test-token',tokenType:'Bearer',expiresIn:3600};
function Home(){const {user}=useAuth();return <><h1>Home page</h1><p>{user?'Signed in':'Signed out'}</p><Link to="/profile">Profile</Link></>;}
function view(path='/register'){return render(<QueryClientProvider client={new QueryClient({defaultOptions:{queries:{retry:false}}})}><MemoryRouter initialEntries={[path]}><AuthProvider><Routes><Route path="/" element={<Home/>}/><Route path="/register" element={<AuthPage key="register" register/>}/><Route path="/login" element={<AuthPage key="login"/>}/><Route path="/profile" element={<Profile/>}/></Routes></AuthProvider></MemoryRouter></QueryClientProvider>);}
function fillLogin(){fireEvent.change(screen.getByLabelText('Email'),{target:{value:user.email}});fireEvent.change(screen.getByLabelText('Password'),{target:{value:'correct-password'}});}
afterEach(()=>{cleanup();setAccessToken(null);vi.unstubAllGlobals();});
describe('account flow',()=>{
 it('registers, opens sign-in, logs in to home and signs out to home clearing the bearer token',async()=>{
  const fetcher=vi.fn().mockResolvedValue({ok:true,json:async()=>response});vi.stubGlobal('fetch',fetcher);view();
  fillLogin();fireEvent.change(screen.getByLabelText('First name'),{target:{value:'Test'}});fireEvent.change(screen.getByLabelText('Last name'),{target:{value:'Traveller'}});fireEvent.change(screen.getByLabelText('Confirm password'),{target:{value:'correct-password'}});
  fireEvent.click(screen.getByRole('button',{name:'Create account →'}));
  expect(await screen.findByRole('status')).toHaveTextContent('Account created.');expect(screen.queryByLabelText('Confirm password')).toBeNull();
  expect((screen.getByLabelText('Email') as HTMLInputElement).value).toBe(user.email);
  const registration=JSON.parse(fetcher.mock.calls[0][1].body);expect(registration).not.toHaveProperty('confirmPassword');
  fireEvent.change(screen.getByLabelText('Password'),{target:{value:'correct-password'}});fireEvent.click(screen.getByRole('button',{name:'Sign in →'}));
  expect(await screen.findByRole('heading',{name:'Home page'})).toBeTruthy();expect(screen.getByText('Signed in')).toBeTruthy();
  fireEvent.click(screen.getByRole('link',{name:'Profile'}));fireEvent.click(screen.getByRole('button',{name:'Sign out'}));
  expect(await screen.findByText('Signed out')).toBeTruthy();await api('/places');
  expect((fetcher.mock.calls.at(-1)![1].headers as Headers).has('Authorization')).toBe(false);
 });
 it('rejects mismatched passwords without creating an account',async()=>{
  const fetcher=vi.fn();vi.stubGlobal('fetch',fetcher);view();fillLogin();fireEvent.change(screen.getByLabelText('First name'),{target:{value:'Test'}});fireEvent.change(screen.getByLabelText('Last name'),{target:{value:'Traveller'}});fireEvent.change(screen.getByLabelText('Confirm password'),{target:{value:'different-password'}});fireEvent.click(screen.getByRole('button',{name:'Create account →'}));
  expect(await screen.findByRole('alert')).toHaveTextContent('Passwords do not match');expect(fetcher).not.toHaveBeenCalled();
 });
 it('keeps failed login on sign-in and shows the backend error',async()=>{
  vi.stubGlobal('fetch',vi.fn().mockResolvedValue({ok:false,status:401,json:async()=>({detail:'Invalid email or password'})}));view('/login');fillLogin();fireEvent.click(screen.getByRole('button',{name:'Sign in →'}));expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password');expect(screen.queryByRole('heading',{name:'Home page'})).toBeNull();
 });
 it('does not send a stale bearer token when signing in',async()=>{
  const fetcher=vi.fn().mockResolvedValue({ok:true,json:async()=>response});vi.stubGlobal('fetch',fetcher);setAccessToken('expired-token');view('/login');fillLogin();fireEvent.click(screen.getByRole('button',{name:'Sign in →'}));await waitFor(()=>expect(fetcher).toHaveBeenCalled());expect((fetcher.mock.calls[0][1].headers as Headers).has('Authorization')).toBe(false);await screen.findByRole('heading',{name:'Home page'});
 });
});
