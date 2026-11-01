import {RouteMap} from '../../components/RouteMap';
import type {PlanPreview} from '../../models/manualPlan';
import type {TripDay} from '../../models/trip';
import type {ManualPlanInput} from '../../models/manualPlan';
export function SavedPlanSummary({preferences,days}:{preferences?:string|null;days?:TripDay[]}) {
 if(!preferences)return null;
 let input:ManualPlanInput & {ai?:{model:string;explanations:string[];warnings:string[]};routePreview?:PlanPreview;savedSchedule?:TripDay[]};try{input=JSON.parse(preferences);}catch{return null;}
 if(typeof input.startingLocation!=='string'||!Array.isArray(input.days))return null;
 const current=!!input.routePreview&&JSON.stringify(input.savedSchedule)===JSON.stringify(days);
 return <aside className="scheduler-notice"><h3>Your planning details</h3>{input.ai&&<><p>AI-assisted draft · {input.ai.model}</p><ul>{[...input.ai.explanations,...input.ai.warnings].map((text,i)=><li key={i}>{text}</li>)}</ul></>}<p>Starting location: {input.startingLocation}</p><p>{input.transportMode?.toLowerCase().replaceAll('_',' ')} · {input.transferMinutes} minutes extra stop buffer</p><ul>{input.days.map(day=><li key={day.date}>{day.date} · {day.area} · {day.startTime.slice(0,5)}–{day.endTime.slice(0,5)}</li>)}</ul>{input.routePreview&&<>{!current?<p className="error">This schedule was edited after route calculation. Previous route estimates may be stale; rebuild a plan to recalculate travel times.</p>:input.routePreview.days.map(day=><div key={day.date}><p>Route for {day.date}</p><RouteMap legs={day.travelLegs||[]} origin={day.origin}/></div>)}</>}<small>Saved route estimates are a snapshot. After editing visits, travel entries or the mode, rebuild the plan to verify travel times again.</small></aside>;
}
