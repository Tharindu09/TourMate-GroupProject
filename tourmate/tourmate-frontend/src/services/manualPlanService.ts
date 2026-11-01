import {api} from '../api/client';
import type {ManualPlanInput, PlanPreview} from '../models/manualPlan';
import type {Trip} from '../models/trip';
export function previewManualPlan(body: ManualPlanInput) {return api<PlanPreview>('/trips/plan/preview',{method:'POST',body:JSON.stringify(body)});}
export function saveManualPlan(body: ManualPlanInput) {return api<Trip>('/trips/plan',{method:'POST',body:JSON.stringify(body)});}

export function getRouteNearby(plan:ManualPlanInput,date:string,at:string,category:'RESTAURANT'|'HOTEL') {
 return api<import('../models/manualPlan').PlanNearbyResponse>('/trips/plan/nearby',{method:'POST',body:JSON.stringify({plan,date,at,category})});
}

let mealQueue:Promise<unknown>=Promise.resolve();
export function getAutomaticMealOptions(plan:ManualPlanInput,date:string,signal?:AbortSignal) {
 const work=mealQueue.then(()=>{
  if(signal?.aborted)throw new DOMException('Request cancelled','AbortError');
  return api<import('../models/manualPlan').PlanMealOptionsResponse>('/trips/plan/meals',{method:'POST',body:JSON.stringify({plan,date}),signal});
 });
 mealQueue=work.catch(()=>undefined);
 return work;
}

export function getAiRouteNearby(plan:ManualPlanInput,date:string,at:string,category:'RESTAURANT'|'HOTEL',preferences:string) {
 return api<{nearby:import('../models/manualPlan').PlanNearbyResponse;reasons:Record<string,string>;model:string}>('/trips/ai/nearby',{method:'POST',body:JSON.stringify({nearby:{plan,date,at,category},preferences})});
}
