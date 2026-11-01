import {useState, type FormEvent} from 'react';
import {useSearchParams} from 'react-router-dom';
import {Failure, Loading} from '../../components/Status';
import {TravelCard} from './TravelCard';
import {searchTravel} from '../../services/travelService';
import {type TravelResults, type TravelSearch} from '../../models/travel';
function dateAfter(days: number) {const date = new Date(); date.setDate(date.getDate() + days); return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;}
export function TravelSearchPage() {
 const [params] = useSearchParams();
 const [q, setQ] = useState(params.get('q') || params.get('location') || ''); const [type, setType] = useState(params.get('type') || 'ATTRACTION');
 const [radius, setRadius] = useState(10000); const [checkin, setCheckin] = useState(dateAfter(7)); const [checkout, setCheckout] = useState(dateAfter(8));
 const [rooms, setRooms] = useState(1); const [adults, setAdults] = useState(2);
 const [results, setResults] = useState<TravelResults | null>(null); const [submitted, setSubmitted] = useState<TravelSearch | null>(null);
 const [busy, setBusy] = useState(false); const [error, setError] = useState<Error | null>(null);
 const hotel = type.endsWith('HOTEL');
 async function load(search: TravelSearch, page = 0) {
  setBusy(true); setError(null);
  try {const data = await searchTravel(search, page); setResults(data); setSubmitted({...search, lat: data.latitude, lon: data.longitude});}
  catch(e) {setError(e instanceof Error ? e : new Error('Travel search failed'));}
  finally {setBusy(false);}
 }
 function submit(event: FormEvent) {event.preventDefault(); if (busy) return; setResults(null); void load({q: q.trim(), type, radius, checkin, checkout, rooms, adults});}
 return <section className="page travel-discovery"><p className="eyebrow">TRAVEL ADVISOR & BOOKING COM · VIA RAPIDAPI</p><h1>Find your next experience.</h1><p>Search the travel providers used by the original TourMate project.</p>
 <form onSubmit={submit}><div className="filter-bar"><label>Destination or town<input required maxLength={150} value={q} onChange={e => setQ(e.target.value)}/></label>
 <label>Category<select value={type} onChange={e => setType(e.target.value)}><option value="ATTRACTION">Attractions</option><option value="RESTAURANT">Restaurants</option><option value="HOTEL">Hotels · Travel Advisor</option><option value="BOOKING_HOTEL">Hotel availability · Booking COM</option><option value="ACTIVITY">Things to do</option></select></label>
 <label>Search area<select value={radius} onChange={e => setRadius(Number(e.target.value))}><option value={5000}>Around 5 km</option><option value={10000}>Around 10 km</option><option value={25000}>Around 25 km</option><option value={50000}>Around 50 km</option></select></label>
 {!hotel && <button className="primary" disabled={busy}>{busy ? 'Searching…' : 'Search travel places →'}</button>}</div>
 {hotel && <div className="filter-bar"><label>Check-in<input required type="date" min={dateAfter(0)} value={checkin} onChange={e => setCheckin(e.target.value)}/></label><label>Check-out<input required type="date" min={checkin} value={checkout} onChange={e => setCheckout(e.target.value)}/></label><label>Rooms<input required type="number" min={1} max={8} value={rooms} onChange={e => setRooms(Number(e.target.value))}/></label><label>Adults<input required type="number" min={rooms} max={30} value={adults} onChange={e => setAdults(Number(e.target.value))}/></label><button className="primary" disabled={busy}>{busy ? 'Searching…' : 'Search hotels →'}</button></div>}
 </form><p className="muted travel-note">Photos, ratings and contact details are shown when supplied by the selected provider. Hotel prices depend on the selected dates and guests. Search areas are bounding boxes.</p>
 {busy && <Loading/>}{error && <Failure error={error}/>}
 {!busy && results && <><p className="muted">{results.places.length} places on this page · Around {results.location}</p><div className="place-grid">{results.places.map(place => <TravelCard key={place.id} place={place}/>)}</div>{!results.places.length && <p className="status">No matching listings returned. Try a larger area or another category.</p>}<div className="pagination"><button disabled={results.page === 0 || busy} onClick={() => submitted && void load(submitted, results.page - 1)}>← Previous</button><span>Page {results.page + 1}</span><button disabled={!results.hasMore || busy} onClick={() => submitted && void load(submitted, results.page + 1)}>Next results →</button></div></>}
 </section>;
}
