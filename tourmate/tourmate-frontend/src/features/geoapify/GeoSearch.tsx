import {useState, type FormEvent} from 'react';
import {useSearchParams} from 'react-router-dom';
import {searchGeoapify} from '../../services/geoapifyService';
import {type GeoResults, type GeoSearchParams} from '../../models/geoapify';
import {GeoAttribution, GeoPlaceCard} from './GeoContent';
import {Failure, Loading} from '../../components/Status';
export function GeoSearch() {
 const [params] = useSearchParams();
 const [location, setLocation] = useState(params.get('q') || params.get('location') || '');
 const [type, setType] = useState(params.get('type') || ''); const [name, setName] = useState(''); const [radius, setRadius] = useState(10000);
 const [submitted, setSubmitted] = useState<GeoSearchParams | null>(null); const [results, setResults] = useState<GeoResults | null>(null);
 const [error, setError] = useState<Error | null>(null); const [busy, setBusy] = useState(false);
 async function load(search: GeoSearchParams, page = 0) {
  setBusy(true); setError(null);
  try {const response = await searchGeoapify(search, page); setResults(response); setSubmitted({...search, lat: response.latitude, lon: response.longitude});}
  catch(e) {setError(e instanceof Error ? e : new Error('Search failed'));}
  finally {setBusy(false);}
 }
 function submit(event: FormEvent) {
  event.preventDefault(); if (busy) return; setResults(null);
  void load({q: location.trim(), type, name: name.trim(), radius});
 }
 return <section className="page geo-discovery"><p className="eyebrow">LIVE PLACE DISCOVERY</p><h1>Explore somewhere new.</h1><p>Find places around any destination with Geoapify.</p>
 <form className="filter-bar" onSubmit={submit}><label>Destination or town<input required value={location} onChange={e => setLocation(e.target.value)} placeholder="City, region or country" maxLength={150}/></label>
 <label>Category<select value={type} onChange={e => setType(e.target.value)}><option value="">All travel places</option><option value="HOTEL">Hotels & guesthouses</option><option value="RESTAURANT">Restaurants & cafes</option><option value="ATTRACTION">Attractions</option><option value="ACTIVITY">Nature & outdoor places</option></select></label>
 <label>Place name (optional)<input value={name} onChange={e => setName(e.target.value)} maxLength={120} placeholder="e.g. Nine Arch"/></label>
 <label>Search radius<select value={radius} onChange={e => setRadius(Number(e.target.value))}><option value={5000}>5 km</option><option value={10000}>10 km</option><option value={25000}>25 km</option><option value={50000}>50 km</option></select></label>
 <button className="primary" disabled={busy}>{busy ? 'Searching…' : 'Search places →'}</button></form>
 <p className="muted geo-search-note">Place data comes from Geoapify. Place photographs load from linked Wikimedia sources when available. Ratings, customer reviews and live prices are unavailable.</p>
 {busy && <Loading/>}{error && <Failure error={error}/>}
 {!busy && results && <><p className="muted">{results.places.length} places on this page · Around {results.location}</p><div className="place-grid">{results.places.map(place => <GeoPlaceCard key={place.id} place={place}/>)}</div>{!results.places.length && <p className="status">No matching places. Try a larger radius, another category or a shorter place name.</p>}<div className="pagination"><button disabled={results.page <= 0 || busy} onClick={() => submitted && void load(submitted, results.page - 1)}>← Previous</button><span>Page {results.page + 1}</span><button disabled={!results.hasMore || busy} onClick={() => submitted && void load(submitted, results.page + 1)}>Next results →</button></div></>}
 <GeoAttribution/>
 </section>;
}
