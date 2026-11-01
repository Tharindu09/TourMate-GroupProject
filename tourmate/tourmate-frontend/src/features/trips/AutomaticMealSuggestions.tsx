import {useEffect,useRef,useState} from 'react';
import type {GeoPlace} from '../../models/geoapify';
import type {ManualPlanInput,MealType,PlanMealOptionsResponse} from '../../models/manualPlan';
import {getAutomaticMealOptions} from '../../services/manualPlanService';
import {Failure} from '../../components/Status';
export function AutomaticMealSuggestions({plan,date,onSelect}:{plan:ManualPlanInput;date:string;onSelect:(place:GeoPlace,meal:MealType,at:string)=>void}) {
 const pending=plan.days.find(d=>d.date===date)?.stops.filter(s=>s.mealType!=='NONE'&&s.placeSource==='CUSTOM'&&(s.latitude===null||s.longitude===null))||[];
 const container=useRef<HTMLDivElement>(null);const [visible,setVisible]=useState(typeof IntersectionObserver==='undefined');
 const [busy,setBusy]=useState(false);const [error,setError]=useState<Error|null>(null);const [result,setResult]=useState<PlanMealOptionsResponse|null>(null);const [attempt,setAttempt]=useState(0);
 const fingerprint=JSON.stringify(plan);
 useEffect(()=>{
  if(!pending.length||!container.current||typeof IntersectionObserver==='undefined')return;
  const observer=new IntersectionObserver(entries=>{setVisible(entries.some(e=>e.isIntersecting));},{rootMargin:'150px'});observer.observe(container.current);return()=>observer.disconnect();
 },[pending.length]);
 useEffect(()=>{
  setResult(null);setError(null);
  if(!pending.length||!visible){setBusy(false);return;}
  const controller=new AbortController();setBusy(true);
  const timer=setTimeout(()=>{void getAutomaticMealOptions(plan,date,controller.signal).then(data=>{if(!controller.signal.aborted)setResult(data);}).catch(e=>{if(!controller.signal.aborted)setError(e instanceof Error?e:new Error('Meal options could not load.'));}).finally(()=>{if(!controller.signal.aborted)setBusy(false);});},650);
  return()=>{clearTimeout(timer);controller.abort();};
 },[fingerprint,date,visible,attempt]);
 if(!pending.length)return null;
 return <section ref={container} className="automatic-meals" aria-label={`Automatic meal suggestions for ${date}`}><h3>Suggested places for your added meals</h3><p className="muted">Options use your selected meal times and the estimated position on the planned route. Choose each venue yourself; the final preview checks its detour and arrival time.</p>
 {busy&&<p role="status">Finding breakfast, lunch and dinner options for your route…</p>}
 {error&&<Failure error={error} retry={()=>setAttempt(a=>a+1)}/>}
 {result?.meals.map(meal=><article className="meal-suggestion-group" key={meal.key}><h4>{meal.mealType.charAt(0)+meal.mealType.slice(1).toLowerCase()} · {meal.at?.slice(0,5)||'Choose a time'}</h4>
 {meal.error?<div className="error" role="alert"><p>{meal.error}</p><button type="button" onClick={()=>setAttempt(a=>a+1)}>Retry meal options</button></div>:meal.suggestions&&<><p>{meal.suggestions.context}</p>{meal.suggestions.warnings.filter(w=>!w.startsWith('Unlocated placeholder')).map(w=><p className="muted" key={w}>{w}</p>)}
 {!meal.suggestions.options.length&&<p>No venues with a route were returned. Change the meal time or use the route picker below.</p>}
 <div className="planner-suggestions">{meal.suggestions.options.map(option=><div key={option.place.id} className="meal-venue"><h4>{option.place.name}</h4><p>{option.place.address}</p><p>About {option.travelMinutes} min · {(option.distanceMeters/1000).toFixed(1)} km from your estimated location</p><p className="muted">Hours: {option.place.openingHours||'Not supplied; confirm with the venue'}</p><button type="button" onClick={()=>onSelect(option.place,meal.mealType,meal.at!)}>Choose for {meal.mealType.toLowerCase()}</button></div>)}</div></>}
 </article>)}
 </section>;
}
