import {useState, type FormEvent} from 'react';
import {useQuery} from '@tanstack/react-query';
import {getDestinations} from '../../services/catalogueService';
import type {TripInput} from '../../models/trip';
export function TripForm({initial, busy, onSave, onCancel, submitLabel, heading}: {submitLabel?: string; heading?: string; initial?: TripInput; busy: boolean; onSave: (value: TripInput) => void; onCancel?: () => void}) {
 const now = new Date(); const today = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')}`;
 const [form, setForm] = useState<TripInput>(initial ? {name:initial.name,startDate:initial.startDate,endDate:initial.endDate,budget:Number(initial.budget),travelers:initial.travelers,destinationIds:[...initial.destinationIds],status:initial.status} : {name: '', startDate: today, endDate: today, budget: 0, travelers: 1, destinationIds: [], status: 'DRAFT'});
 const destinations = useQuery({queryKey: ['tripDestinations'], queryFn: () => getDestinations('size=100')});
 function submit(e: FormEvent) {e.preventDefault(); onSave({...form, name: form.name.trim()});}
 return <form className="scheduler-form" onSubmit={submit}><fieldset disabled={busy}><legend>{heading || (initial ? 'Edit trip' : 'Create a trip')}</legend><div className="scheduler-fields">
 <label>Trip name<input required maxLength={160} placeholder="My next adventure" value={form.name} onChange={e=>setForm({...form,name:e.target.value})}/></label>
 <label>Start date<input required type="date" value={form.startDate} onChange={e=>setForm({...form,startDate:e.target.value})}/></label>
 <label>End date<input required type="date" min={form.startDate} value={form.endDate} onChange={e=>setForm({...form,endDate:e.target.value})}/></label>
 <label>Total budget (LKR)<input required type="number" min="0" max="9999999999.99" step="0.01" value={form.budget} onChange={e=>setForm({...form,budget:Number(e.target.value)})}/></label>
 <label>Travelers<input required type="number" min="1" max="100" value={form.travelers} onChange={e=>setForm({...form,travelers:Number(e.target.value)})}/></label>
 <label>Trip status<select value={form.status} onChange={e=>setForm({...form,status:e.target.value as TripInput['status']})}><option value="DRAFT">Draft</option><option value="PLANNED">Planned</option><option value="COMPLETED">Completed</option></select></label></div>
 <fieldset className="destination-choices"><legend>Destinations (optional)</legend>{destinations.data?.content.map(d=><label key={d.id}><input type="checkbox" checked={form.destinationIds.includes(d.id)} onChange={e=>setForm({...form,destinationIds:e.target.checked?[...form.destinationIds,d.id]:form.destinationIds.filter(id=>id!==d.id)})}/>{d.name}</label>)}{destinations.isError && <p>Destinations could not load. You can still create a trip.</p>}</fieldset>
 <p className="muted">Plan 1–60 days. Costs are total amounts for your group, in LKR.</p><div className="scheduler-actions"><button className="primary" disabled={busy}>{busy?'Saving…':submitLabel || (initial?'Save trip':'Create trip →')}</button>{onCancel&&<button type="button" onClick={onCancel}>Cancel</button>}</div></fieldset></form>;
}
