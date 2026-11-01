import {useEffect,useRef,useState} from 'react';
import type {GeoPlace} from '../../models/geoapify';
import type {ManualPlanInput,MealType,PlanNearbyResponse} from '../../models/manualPlan';
import {getRouteNearby,getAiRouteNearby} from '../../services/manualPlanService';
import {Failure} from '../../components/Status';
import {PlaceMap} from '../../components/PlaceMap';
export function RouteNearbyPicker({plan,date,onSelect}:{plan:ManualPlanInput;date:string;onSelect:(place:GeoPlace,meal:MealType,time:string)=>void}) {
 const day=plan.days.find(d=>d.date===date)!;
 const [kind,setKind]=useState<'RESTAURANT'|'HOTEL'>('RESTAURANT');const [meal,setMeal]=useState<MealType>('LUNCH');const [at,setAt]=useState('12:30');
 const [useAi,setUseAi]=useState(false);const [preferences,setPreferences]=useState('A practical nearby stop with a short detour.');const [reasons,setReasons]=useState<Record<string,string>>({});
 const [busy,setBusy]=useState(false);const [result,setResult]=useState<PlanNearbyResponse|null>(null);const [error,setError]=useState<Error|null>(null);const request=useRef(0);
 const fingerprint=JSON.stringify(plan);
 useEffect(()=>{request.current++;setResult(null);setReasons({});setError(null);setBusy(false);},[fingerprint]);
 function reset(){request.current++;setResult(null);setReasons({});setError(null);setBusy(false);}
 async function find(){const current=++request.current;setBusy(true);setError(null);setResult(null);try{const ranked=useAi?await getAiRouteNearby(plan,date,at,kind,preferences):null;const response=ranked?.nearby??await getRouteNearby(plan,date,at,kind);if(current===request.current){setResult(response);setReasons(ranked?.reasons??{});}}catch(e){if(current===request.current)setError(e as Error);}finally{if(current===request.current)setBusy(false);}}
 return <div className="route-nearby"><h3>Meal and hotel options along this day's route</h3><p className="muted">Choose a time. TourMate estimates where you will be on the mapped route and finds reachable nearby venues. Select a venue, then rebuild to check the detour.</p>
 <label><input type="checkbox" checked={useAi} onChange={e=>{setUseAi(e.target.checked);reset();}}/>Rank nearby options with AI</label>{useAi&&<label>Meal or hotel preferences<textarea maxLength={2000} value={preferences} onChange={e=>{setPreferences(e.target.value);reset();}} placeholder="Dietary needs, cuisine, accessibility or hotel preferences"/></label>}
 <div className="scheduler-fields"><label>Nearby venue type<select value={kind} onChange={e=>{const type=e.target.value as typeof kind;setKind(type);if(type==='HOTEL'){setMeal('NONE');setAt(day.endTime);}else {setMeal('LUNCH');setAt('12:30');}reset();}}><option value="RESTAURANT">Restaurants for meals</option><option value="HOTEL">Hotels near your planned location</option></select></label>
 <label>Purpose<select value={meal} onChange={e=>{const value=e.target.value as MealType;setMeal(value);setAt(value==='BREAKFAST'?'08:00':value==='LUNCH'?'12:30':value==='DINNER'?'19:00':day.endTime);reset();}}>{kind==='HOTEL'&&<option value="NONE">Overnight / check-in</option>}<option value="BREAKFAST">Breakfast</option><option value="LUNCH">Lunch</option><option value="DINNER">Dinner</option></select></label>
 <label>Find options near me at<input type="time" value={at} onChange={e=>{setAt(e.target.value);reset();}}/></label></div><button type="button" disabled={busy||!at||(useAi&&!preferences.trim())} onClick={()=>void find()}>{busy?'Tracing route and finding options…':useAi?'Find AI-ranked nearby options':'Find nearby route options'}</button>
 {error&&<Failure error={error}/>} {result&&<><p><strong>{result.at.slice(0,5)}:</strong> {result.context}</p><PlaceMap place={{name:result.anchor.label||'Estimated route position',latitude:result.anchor.latitude,longitude:result.anchor.longitude}}/>
 {result.warnings.map(w=><p className="muted" key={w}>{w}</p>)}{!result.options.length&&<p>No routable venues were returned nearby. Try another time or mode.</p>}<div className="planner-suggestions">{result.options.map(option=><article key={option.place.id}><h3>{option.place.name}</h3>{reasons[option.place.id]&&<p><strong>AI suggestion:</strong> {reasons[option.place.id]}</p>}<p>{option.place.address}</p><p className="muted">Hours: {option.place.openingHours||'Not supplied; confirm with the venue'}</p><p>About {option.travelMinutes} min · {(option.distanceMeters/1000).toFixed(1)} km from the estimated position</p>{kind==='HOTEL'&&meal!=='NONE'&&<p className="muted">Confirm that the hotel serves meals to non-guests.</p>}<button type="button" onClick={()=>{onSelect(option.place,meal,at);reset();}}>Select {meal==='NONE'?'hotel':meal.toLowerCase()+' venue'}</button></article>)}</div></>}
 </div>;
}
