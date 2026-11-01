import {useState, type FormEvent} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {useQuery} from '@tanstack/react-query';
import {api} from '../api/client';
import type {Destination, Page, Place} from '../types';
import {Loading, Failure} from '../components/Status';
import {PlaceCard} from '../components/PlaceCard';
import {scenery} from '../utils/format';
export function Home() {
 const [q, setQ] = useState(''); const navigate = useNavigate();
 const destinations = useQuery({queryKey: ['destinations'], queryFn: () => api<Page<Destination>>('/destinations?size=7')});
 const places = useQuery({queryKey: ['featured'], queryFn: () => api<Page<Place>>('/places?type=ATTRACTION&size=4')});
 function search(e: FormEvent) {e.preventDefault(); navigate(`/search?q=${encodeURIComponent(q.trim())}`);}
 return <>
  <section className="hero"><div className="eyebrow">SMALL ISLAND. ENDLESS POSSIBILITIES.</div><h1>A little closer to<br/><em>somewhere new.</em></h1><p>Find your kind of Sri Lanka. The places, the flavours,<br className="desktop"/> and the moments you’ll take home.</p>
  <form className="search-bar" onSubmit={search}><span aria-hidden="true">⌕</span><input aria-label="Search destinations" value={q} onChange={e => setQ(e.target.value)} placeholder="Where would you like to go?"/><button className="primary">Explore →</button></form>
  <div className="quick-links"><Link to="/search?type=HOTEL">⌂ Stays</Link><Link to="/search?type=RESTAURANT">♧ Eat & drink</Link><Link to="/search?type=ATTRACTION">◈ Things to do</Link><Link to="/search?type=ACTIVITY">↗ Adventures</Link></div></section>
  <section><div className="section-heading"><div><p className="eyebrow">YOUR NEXT CHAPTER</p><h2>One island. A world to discover.</h2></div><Link to="/search">Explore all destinations ↗</Link></div>
  {destinations.isPending ? <Loading/> : destinations.isError ? <Failure error={destinations.error} retry={() => void destinations.refetch()}/> : <div className="destination-grid">{destinations.data.content.map(d => <Link key={d.id} to={`/destinations/${d.id}`} className={`destination landscape ${scenery(d.id)}`}><span className="terrain" aria-hidden="true"/><div><span className="destination-kicker">SRI LANKA</span><h3>{d.name}</h3></div><span className="round-arrow" aria-hidden="true">↗</span></Link>)}</div>}</section>
  <section className="feature-banner"><div><p className="eyebrow">LESS RUSH. MORE WONDER.</p><h2>Take the scenic route.</h2><p>Start with a place that speaks to you.<br/>There’s a whole island waiting beyond it.</p><Link className="primary button" to="/destinations/1">Discover Ella ↗</Link></div><div className="banner-art landscape hill" aria-hidden="true"><span className="terrain"/><span className="art-caption">6.8667° N &nbsp; 81.0466° E<br/>Ella, Sri Lanka</span></div></section>
  <section><div className="section-heading"><div><p className="eyebrow">GO A LITTLE FURTHER</p><h2>Places for your next adventure</h2></div><Link to="/search?type=ATTRACTION">See all places ↗</Link></div>{places.isPending ? <Loading/> : places.isError ? <Failure error={places.error}/> : <div className="place-grid">{places.data.content.map(p => <PlaceCard key={p.id} place={p}/>)}</div>}</section>
  <div className="note">Made for curious travellers. Built around Sri Lanka.</div>
 </>;
}
