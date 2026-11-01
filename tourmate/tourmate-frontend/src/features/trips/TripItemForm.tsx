import {useState, type FormEvent} from 'react';
import {getPlaces} from '../../services/catalogueService';
import type {Place} from '../../models';
import type {SchedulePlace, TripDay, TripItem, TripItemInput} from '../../models/trip';
export function TripItemForm({days, dayId, initial, selectedPlace, busy, onSave, onCancel}: {days: TripDay[]; dayId: number; initial?: TripItem; selectedPlace?: SchedulePlace; busy: boolean; onSave: (value: TripItemInput) => void; onCancel: () => void}) {
 const [form, setForm] = useState<TripItemInput>(initial || {dayId,title:'',category:'ACTIVITY',placeSource:'CUSTOM',placeId:null,address:null,latitude:null,longitude:null,startTime:'09:00',endTime:'10:00',estimatedCost:0,notes:'',...selectedPlace});
 const [q,setQ]=useState('');const [searched,setSearched]=useState(false);const [results,setResults]=useState<Place[]>([]);const [finding,setFinding]=useState(false);const [searchError,setSearchError]=useState('');
 async function search() {setFinding(true);setSearched(true);setSearchError('');try {setResults((await getPlaces(new URLSearchParams({q,size:'12'}).toString())).content);}catch(e){setSearchError((e as Error).message);}finally{setFinding(false);}}
 function select(p: Place) {setForm({...form,title:p.name,category:p.type,placeSource:'TOURMATE',placeId:String(p.id),address:p.address,latitude:p.latitude,longitude:p.longitude,estimatedCost:Number(p.estimatedCost)});setResults([]);}
 function submit(e: FormEvent) {e.preventDefault();onSave({...form,title:form.title.trim()});}
 return <form className="scheduler-form activity-form" onSubmit={submit}><fieldset disabled={busy}><legend>{initial?'Edit activity':'Add activity'}</legend>
 <div className="place-picker"><label>Find a TourMate place<input value={q} maxLength={150} onChange={e=>setQ(e.target.value)} placeholder="Nine Arch Bridge"/></label><button type="button" onClick={()=>void search()} disabled={finding}>{finding?'Searching…':'Find places'}</button><p className="muted">You can also use a custom activity, or choose “Add to trip” on a travel search result.</p>{searchError&&<p role="alert">{searchError}</p>}
 {searched&&!finding&&!searchError&&!results.length&&<p className="muted">No places found. Try another name or enter a custom activity below.</p>}{results.length>0&&<ul className="place-picker-results">{results.map(p=><li key={p.id}><span>{p.name}<small>{p.address}</small></span><button type="button" onClick={()=>select(p)}>Select</button></li>)}</ul>}</div>
 <p className="muted">{form.placeSource==='CUSTOM'?'Custom activity':`Selected place: ${form.title}`}{form.placeSource!=='CUSTOM'&&<button type="button" className="small-button" onClick={()=>setForm({...form,placeSource:'CUSTOM',placeId:null,address:null,latitude:null,longitude:null})}>Use custom activity</button>}</p>
 <div className="scheduler-fields"><label>Activity name<input required maxLength={300} value={form.title} onChange={e=>setForm({...form,title:e.target.value})}/></label>
 <label>Day<select value={form.dayId} onChange={e=>setForm({...form,dayId:Number(e.target.value)})}>{days.map(d=><option key={d.id} value={d.id}>Day {d.dayNumber} · {d.date}</option>)}</select></label>
 <label>Category<select value={form.category} onChange={e=>setForm({...form,category:e.target.value as TripItemInput['category']})}>{['ATTRACTION','ACTIVITY','RESTAURANT','HOTEL','TRANSPORT','OTHER'].map(c=><option key={c} value={c}>{c.charAt(0)+c.slice(1).toLowerCase()}</option>)}</select></label>
 <label>Start time<input required type="time" value={form.startTime.slice(0,5)} onChange={e=>setForm({...form,startTime:e.target.value})}/></label>
 <label>End time<input required type="time" min={form.startTime.slice(0,5)} value={form.endTime.slice(0,5)} onChange={e=>setForm({...form,endTime:e.target.value})}/></label>
 <label>Estimated cost (LKR)<input required type="number" min="0" max="9999999999.99" step="0.01" value={form.estimatedCost} onChange={e=>setForm({...form,estimatedCost:Number(e.target.value)})}/></label></div>
 <label>Notes<textarea maxLength={4000} rows={3} value={form.notes} onChange={e=>setForm({...form,notes:e.target.value})}/></label>
 <p className="muted">Times use the destination’s local time. Overlapping activities cannot be saved. Enter costs in LKR; provider prices are not converted.</p>
 <div className="scheduler-actions"><button className="primary">{busy?'Saving…':'Save activity'}</button><button type="button" onClick={onCancel}>Cancel</button></div></fieldset></form>;
}
