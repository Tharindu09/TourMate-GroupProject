import {Link, useSearchParams} from 'react-router-dom';
import {useQuery} from '@tanstack/react-query';
import {api} from '../api/client';
import type {Destination, Page, Place} from '../types';
import {Loading, Failure} from '../components/Status';
import {PlaceCard} from '../components/PlaceCard';
export function Search() {
 const [params, setParams] = useSearchParams();
 const q = params.get('q') ?? ''; const page = Number(params.get('page') ?? '0');
 const query = new URLSearchParams(params); query.set('size', '12');
 const places = useQuery({queryKey: ['places', query.toString()], queryFn: () => api<Page<Place>>(`/places?${query}`)});
 const destinations = useQuery({queryKey: ['allDestinations'], queryFn: () => api<Page<Destination>>('/destinations?size=100')});
 const matches = destinations.data?.content.filter(d => `${d.name} ${d.city} ${d.country}`.toLowerCase().includes(q.toLowerCase().trim())) ?? [];
 function change(key: string, value: string) {const next = new URLSearchParams(params); if (value) next.set(key, value); else next.delete(key); next.delete('page'); setParams(next, {replace: true});}
 return <section className="page"><p className="eyebrow">FIND YOUR KIND OF PLACE</p><h1>Explore Sri Lanka</h1>
 <div className="filter-bar"><label>Search places<input value={q} onChange={e => change('q', e.target.value)} placeholder="Name of a place"/></label><label>Destination<select value={params.get('destinationId') ?? ''} onChange={e => change('destinationId', e.target.value)}><option value="">All destinations</option>{destinations.data?.content.map(d => <option value={d.id} key={d.id}>{d.name}</option>)}</select></label><label>Category<select value={params.get('type') ?? ''} onChange={e => change('type', e.target.value)}><option value="">Everything</option><option value="HOTEL">Stays</option><option value="RESTAURANT">Eat & drink</option><option value="ATTRACTION">Things to do</option><option value="ACTIVITY">Activities</option></select></label><label>Max cost (LKR)<input type="number" min="0" value={params.get('maxPrice') ?? ''} onChange={e => change('maxPrice', e.target.value)}/></label><label>Min rating<select value={params.get('minRating') ?? ''} onChange={e => change('minRating', e.target.value)}><option value="">Any rating</option><option value="3">3+</option><option value="4">4+</option><option value="4.5">4.5+</option></select></label></div>
 {destinations.data && q && <div className="destination-matches">Destinations: {matches.length ? matches.map(d => <Link key={d.id} to={`/destinations/${d.id}`}>{d.name} ↗</Link>) : 'No matching destinations'}</div>}
 {places.isPending ? <Loading/> : places.isError ? <Failure error={places.error} retry={() => void places.refetch()}/> : <><p className="muted">{places.data.totalElements} places to discover</p><div className="place-grid">{places.data.content.map(p => <PlaceCard key={p.id} place={p}/>)}</div>{places.data.content.length === 0 && <div className="status">No places match these filters. Try another destination or category.</div>}<div className="pagination"><button disabled={page <= 0} onClick={() => {const n = new URLSearchParams(params); n.set('page', String(page - 1)); setParams(n);}}>← Previous</button><span>Page {places.data.totalPages ? page + 1 : 0} of {places.data.totalPages}</span><button disabled={page + 1 >= places.data.totalPages} onClick={() => {const n = new URLSearchParams(params); n.set('page', String(page + 1)); setParams(n);}}>Next →</button></div></>}
 </section>;
}
