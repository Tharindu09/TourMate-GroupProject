import {useState} from 'react';
import {Link, useLocation, useNavigate} from 'react-router-dom';
import {useQuery, useQueryClient} from '@tanstack/react-query';
import {useAuth} from '../auth/AuthContext';
import {createTrip, getTrips} from '../../services/tripService';
import type {SchedulePlace, TripInput} from '../../models/trip';
import {Failure, Loading} from '../../components/Status';
import {TripForm} from './TripForm';
import {tripMoney as money} from '../../utils/trip';
export function TripsPage() {
 const {user}=useAuth();const location=useLocation();const navigate=useNavigate();const client=useQueryClient();
 const selectedPlace=(location.state as {place?:SchedulePlace}|null)?.place;
 const [page,setPage]=useState(0);const [creating,setCreating]=useState(false);const [busy,setBusy]=useState(false);const [error,setError]=useState<Error|null>(null);
 const trips=useQuery({queryKey:['trips',user?.id,page],queryFn:()=>getTrips(page),enabled:!!user});
 async function save(body:TripInput) {setBusy(true);setError(null);try {const trip=await createTrip(body);await client.invalidateQueries({queryKey:['trips']});navigate(`/trips/${trip.id}`,{state:{place:selectedPlace}});}catch(e){setError(e as Error);}finally{setBusy(false);}}
 if(!user)return <section className="page"><p className="eyebrow">YOUR TRAVEL DAYS, YOUR WAY</p><h1>Plan a little adventure.</h1><p>Sign in to create and save your personal trip schedules.</p><Link className="primary" to="/login" state={{returnTo:'/trips',place:selectedPlace}}>Sign in to plan →</Link></section>;
 return <section className="page trips-page"><p className="eyebrow">THE MANUAL TRIP PLANNER</p><div className="section-heading"><div><h1>Your trips.</h1><p>Make room for the places you want to see.</p></div><div className="scheduler-actions"><Link className="primary" to="/trips/plan" state={{place:selectedPlace}}>+ Plan a trip</Link><Link className="primary" to="/trips/ai">Plan with AI</Link><button onClick={()=>setCreating(true)}>Create empty trip</button></div></div>
 {selectedPlace&&<div className="scheduler-notice">Add <strong>{selectedPlace.title}</strong> to a trip below, or create a new one.</div>}
 {error&&<Failure error={error}/>} {creating&&<TripForm busy={busy} onSave={body=>void save(body)} onCancel={()=>setCreating(false)}/>}
 {trips.isPending?<Loading/>:trips.isError?<Failure error={trips.error} retry={()=>void trips.refetch()}/>:<>{!trips.data.content.length&&<div className="scheduler-empty"><h2>A fresh page for your next trip.</h2><p>Create a trip, choose your dates, then build each day with places and custom activities.</p></div>}
 <div className="trip-grid">{trips.data.content.map(trip=><Link className="trip-summary" key={trip.id} to={`/trips/${trip.id}`} state={{place:selectedPlace}}><span className="eyebrow">{trip.status.toLowerCase()}</span><h2>{trip.name}</h2><p>{trip.startDate} → {trip.endDate}</p><p className="muted">{trip.travelers} traveler{trip.travelers===1?'':'s'} · {money(trip.budget)} budget</p><span className="text-link">{selectedPlace?'Choose trip':'Open schedule'} →</span></Link>)}</div>
 {trips.data.totalPages>1&&<div className="pagination"><button disabled={page===0} onClick={()=>setPage(p=>p-1)}>← Previous</button><span>Page {page+1}</span><button disabled={page+1>=trips.data.totalPages} onClick={()=>setPage(p=>p+1)}>Next →</button></div>}</>}
 </section>;
}
