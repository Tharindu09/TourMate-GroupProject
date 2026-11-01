import type {Place} from '../models';
import type {GeoPlace} from '../models/geoapify';
import type {TravelPlace} from '../models/travel';
import type {SchedulePlace} from '../models/trip';
export function localSchedulePlace(p: Place): SchedulePlace {return {title:p.name,category:p.type,placeSource:'TOURMATE',placeId:String(p.id),address:p.address,latitude:p.latitude,longitude:p.longitude};}
export function geoSchedulePlace(p: GeoPlace): SchedulePlace {return {title:p.name,category:p.categories.some(c=>c.startsWith('catering'))?'RESTAURANT':p.categories.some(c=>c.startsWith('accommodation'))?'HOTEL':'ATTRACTION',placeSource:'GEOAPIFY',placeId:p.id,address:p.address,latitude:p.latitude,longitude:p.longitude};}
export function travelSchedulePlace(p: TravelPlace): SchedulePlace {return {title:p.name,category:p.type.endsWith('HOTEL')?'HOTEL':p.type==='RESTAURANT'?'RESTAURANT':p.type==='ACTIVITY'?'ACTIVITY':'ATTRACTION',placeSource:'RAPIDAPI',placeId:p.id,address:p.address,latitude:p.latitude,longitude:p.longitude};}
export const tripMoney=(value:number)=>new Intl.NumberFormat('en-LK',{style:'currency',currency:'LKR',minimumFractionDigits:2,maximumFractionDigits:2}).format(value);
