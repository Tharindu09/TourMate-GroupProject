import {Link, NavLink, Route, Routes} from 'react-router-dom';
import {useAuth} from './features/auth/AuthContext';
import {Home} from './pages/Home';
import {Search} from './pages/Search';
import {DestinationDetails, PlaceDetails} from './pages/Details';
import {AuthPage, Profile} from './pages/Auth';
export default function App() {
 const {user} = useAuth();
 return <><header><Link className="logo" to="/" aria-label="TourMate home"><span className="logo-mark">◒</span> tourmate<span className="logo-dot">.</span></Link><nav aria-label="Main navigation"><NavLink to="/search">Discover</NavLink><NavLink to="/destinations/1">Explore Ella</NavLink></nav><Link className="nav-account" to={user ? '/profile' : '/login'}>{user ? user.firstName : 'Sign in'} <span aria-hidden="true">↗</span></Link></header><main><Routes><Route path="/" element={<Home/>}/><Route path="/search" element={<Search/>}/><Route path="/destinations/:id" element={<DestinationDetails/>}/><Route path="/places/:id" element={<PlaceDetails/>}/><Route path="/login" element={<AuthPage/>}/><Route path="/register" element={<AuthPage register/>}/><Route path="/profile" element={<Profile/>}/><Route path="*" element={<section className="page"><h1>A little off the beaten path.</h1><Link to="/">Back to home →</Link></section>}/></Routes></main><footer><Link className="logo" to="/">tourmate.</Link><p>A little curiosity goes a long way.</p><span>Discover Sri Lanka · Travel thoughtfully</span></footer></>;
}
