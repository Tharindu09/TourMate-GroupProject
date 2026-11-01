import {SavedPlanSummary} from './SavedPlanSummary';
import {useEffect, useRef, useState, type DragEvent} from 'react';
import {Link, useLocation, useNavigate, useParams} from 'react-router-dom';
import {useQuery, useQueryClient} from '@tanstack/react-query';
import {useAuth} from '../auth/AuthContext';
import {deleteTrip, deleteTripItem, getTrip, moveTripItem, reorderTripDay, saveTripItem, updateTrip} from '../../services/tripService';
import type {SchedulePlace, Trip, TripDay, TripInput, TripItem, TripItemInput} from '../../models/trip';
import {Failure, Loading} from '../../components/Status';
import {PlaceMap} from '../../components/PlaceMap';
import {TripForm} from './TripForm';
import {TripItemForm} from './TripItemForm';
import {tripMoney as money} from '../../utils/trip';
export function TripDetails() {
 const {id=''}=useParams();const {user}=useAuth();const location=useLocation();const navigate=useNavigate();const client=useQueryClient();
 const editorRef=useRef<HTMLDivElement>(null); const errorRef=useRef<HTMLDivElement>(null);
 const [selectedPlace,setSelectedPlace]=useState<SchedulePlace|undefined>((location.state as {place?:SchedulePlace}|null)?.place);
 const [editor,setEditor]=useState<{dayId:number;item?:TripItem}|null>(null);const [editTrip,setEditTrip]=useState(false);const [confirmDelete,setConfirmDelete]=useState(false);
 const [busy,setBusy]=useState(false);const [error,setError]=useState<Error|null>(null);const [message,setMessage]=useState('');const [dragging,setDragging]=useState<TripItem|null>(null);
 const key=['trip',id,user?.id];const query=useQuery({queryKey:key,queryFn:()=>getTrip(id),enabled:!!user});
 useEffect(()=>{if(selectedPlace&&query.data?.days[0]&&!editor){setEditor({dayId:query.data.days[0].id});navigate(location.pathname,{replace:true,state:null});}},[selectedPlace,query.data]);
 useEffect(()=>{if(editor)editorRef.current?.scrollIntoView?.({behavior:'smooth',block:'start'});},[editor]);
 useEffect(()=>{if(error)errorRef.current?.scrollIntoView?.({behavior:'smooth',block:'nearest'});},[error]);
 async function mutate(action:()=>Promise<Trip>, done?:()=>void) {
  if(busy)return;setBusy(true);setError(null);setMessage('');
  try{const trip=await action();client.setQueryData(key,trip);void client.invalidateQueries({queryKey:['trips']});done?.();setMessage('Schedule saved.');}catch(e){setError(e as Error);}finally{setBusy(false);}
 }
 async function removeTrip() {if(!query.data||busy)return;setBusy(true);setError(null);try{await deleteTrip(query.data.id);await client.invalidateQueries({queryKey:['trips']});navigate('/trips');}catch(e){setError(e as Error);setBusy(false);}}
 function closeEditor(){setEditor(null);setSelectedPlace(undefined);}
 function drop(e:DragEvent,day:TripDay,beforeId?:number) {
  e.preventDefault();e.stopPropagation();if(!dragging||!query.data||busy)return;
  if(beforeId===dragging.id){setDragging(null);return;}
  const ids=day.items.filter(i=>i.id!==dragging.id).map(i=>i.id);const index=beforeId===undefined?ids.length:ids.indexOf(beforeId);
  if(index<0)return;const item=dragging;setDragging(null);
  if(item.dayId===day.id){ids.splice(index,0,item.id);void mutate(()=>reorderTripDay(query.data!.id,day.id,ids));}
  else void mutate(()=>moveTripItem(query.data!.id,item.id,day.id,index));
 }
 function shift(day:TripDay,index:number,offset:number) {if(!query.data)return;const ids=day.items.map(i=>i.id);[ids[index],ids[index+offset]]=[ids[index+offset],ids[index]];void mutate(()=>reorderTripDay(query.data!.id,day.id,ids));}
 if(!user)return <section className="page"><h1>Your private trip schedule.</h1><Link className="text-link" to="/login" state={{returnTo:`/trips/${id}`}}>Sign in to view your trip →</Link></section>;
 if(query.isPending)return <Loading/>;if(query.isError)return <Failure error={query.error} retry={()=>void query.refetch()}/>;
 const trip=query.data;
 const total=Number(trip.estimatedTotal);const remaining=Number(trip.remainingBudget);
 return <section className="page trip-detail"><Link to="/trips">← My trips</Link><p className="eyebrow detail-label">{trip.status} · MANUAL PLAN</p><h1>{trip.name}</h1><p className="intro">{trip.startDate} → {trip.endDate} · {trip.travelers} traveler{trip.travelers===1?'':'s'}</p>
 <div className="scheduler-actions"><Link className="text-link" to="/trips/plan">Plan another trip step by step →</Link><button disabled={busy} onClick={()=>setEditTrip(v=>!v)}>Edit trip</button><Link className="text-link" to="/search">Discover places to add ↗</Link><button className="danger-button" disabled={busy} onClick={()=>setConfirmDelete(true)}>Delete trip</button></div>
 {confirmDelete&&<div className="scheduler-notice" role="alert"><p>Delete “{trip.name}” and all its activities? This cannot be undone.</p><button disabled={busy} onClick={()=>void removeTrip()}>Delete permanently</button> <button disabled={busy} onClick={()=>setConfirmDelete(false)}>Keep trip</button></div>}
 {error&&<div ref={errorRef}><Failure error={error}/></div>} {message&&<p role="status" className="scheduler-success">{message}</p>}
 {editTrip&&<TripForm initial={trip} busy={busy} onSave={(body:TripInput)=>void mutate(()=>updateTrip(trip.id,body),()=>{setEditTrip(false);closeEditor();})} onCancel={()=>setEditTrip(false)}/>}
 <SavedPlanSummary preferences={trip.planningPreferences} days={trip.days}/><div className="trip-budget" aria-label="Trip budget"><div><span>Budget</span><strong>{money(trip.budget)}</strong></div><div><span>Estimated total</span><strong>{money(total)}</strong></div><div className={remaining<0?'over-budget':''}><span>{remaining<0?'Over budget':'Remaining'}</span><strong>{money(Math.abs(remaining))}</strong></div></div>
 {Object.keys(trip.costByCategory).length>0&&<div className="budget-categories">{Object.entries(trip.costByCategory).map(([category,cost])=><span key={category}>{category.toLowerCase()}: {money(cost)}</span>)}</div>}
 <p className="muted scheduler-help">Drag activities to reorder or move between days. Use ↑ / ↓ buttons or Edit → Day on touch screens and with a keyboard. Times are the destination’s local time; moving an activity keeps its times.</p>
 {editor&&<div ref={editorRef} className="activity-editor"><TripItemForm key={`${editor.dayId}-${editor.item?.id||'new'}`} days={trip.days} dayId={editor.dayId} initial={editor.item} selectedPlace={selectedPlace} busy={busy} onSave={(body:TripItemInput)=>void mutate(()=>saveTripItem(trip.id,body,editor.item?.id),closeEditor)} onCancel={closeEditor}/></div>}
 <div className="trip-timeline">{trip.days.map(day=><section key={day.id} className={`trip-day${dragging?' drop-ready':''}`} aria-label={`Day ${day.dayNumber}`} onDragOver={e=>{if(dragging&&!busy)e.preventDefault();}} onDrop={e=>drop(e,day)}>
 <div className="trip-day-heading"><div><p className="eyebrow">DAY {day.dayNumber}</p><h2>{new Date(`${day.date}T12:00:00`).toLocaleDateString('en-GB',{weekday:'long',day:'numeric',month:'short'})}</h2></div><button disabled={busy} onClick={()=>{setSelectedPlace(undefined);setEditor({dayId:day.id});setError(null);}}>+ Add activity</button></div>
 {!day.items.length&&<p className="scheduler-empty">A little space for something memorable. Add an activity{dragging?' or drop one here':''}.</p>}
 <ol className="trip-activities">{day.items.map((item,index)=><li key={item.id} draggable={!busy} onDragStart={e=>{setDragging(item);e.dataTransfer.setData('text/plain',String(item.id));e.dataTransfer.effectAllowed='move';}} onDragEnd={()=>setDragging(null)} onDragOver={e=>{if(dragging&&!busy)e.preventDefault();}} onDrop={e=>drop(e,day,item.id)}>
 <div className="activity-time"><span draggable={!busy} title={`Drag ${item.title} to another position or day`} aria-label={`Drag ${item.title}`} className="drag-handle">⠿</span><strong>{item.startTime.slice(0,5)}</strong><span>{item.endTime.slice(0,5)}</span></div><div className="activity-body"><p className="eyebrow">{item.mealType&&item.mealType!=='NONE'?item.mealType.toLowerCase():item.category.toLowerCase()}</p><h3>{item.title}</h3>{item.address&&<p className="muted">{item.address}</p>}<p>{money(item.estimatedCost)}</p>{item.notes&&<p className="activity-notes">{item.notes}</p>}{item.latitude!==null&&item.longitude!==null&&<PlaceMap place={{name:item.title,address:item.address,latitude:item.latitude,longitude:item.longitude}}/>}
 <div className="scheduler-actions"><button disabled={busy} onClick={()=>{setSelectedPlace(undefined);setEditor({dayId:day.id,item});}}>Edit {item.title}</button><button disabled={busy||index===0} aria-label={`Move ${item.title} up`} onClick={()=>shift(day,index,-1)}>↑</button><button disabled={busy||index===day.items.length-1} aria-label={`Move ${item.title} down`} onClick={()=>shift(day,index,1)}>↓</button><button disabled={busy} className="danger-button" aria-label={`Remove ${item.title}`} onClick={()=>void mutate(()=>deleteTripItem(trip.id,item.id))}>Remove</button></div></div></li>)}</ol>
 </section>)}</div></section>;
}
