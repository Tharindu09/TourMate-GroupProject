import {useEffect,useRef,useState} from 'react';
import type {RoutePoint,TravelLeg} from '../models/manualPlan';
import 'leaflet/dist/leaflet.css';
export function RouteMap({legs,origin}:{legs:TravelLeg[];origin?:RoutePoint}) {
 const [open,setOpen]=useState(false);const container=useRef<HTMLDivElement>(null);
 useEffect(()=>{
  if(!open||!container.current)return;let disposed=false;let instance:import('leaflet').Map|undefined;
  void import('leaflet').then(L=>{
   if(disposed||!container.current)return;const map=L.map(container.current,{scrollWheelZoom:false});instance=map;
   L.tileLayer(import.meta.env.VITE_OSM_TILE_URL||'https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>'}).addTo(map);
   const points:import('leaflet').LatLngTuple[]=[];
   function marker(point:RoutePoint,label:string){const text=document.createElement('span');text.textContent=label;L.circleMarker([point.latitude,point.longitude],{radius:7,color:'#174d38',fillColor:'#db8e42',fillOpacity:1}).bindPopup(text).addTo(map);points.push([point.latitude,point.longitude]);}
   if(origin)marker(origin,`Start: ${origin.label}`);
   legs.forEach((leg,i)=>{const line=leg.route.geometry.map(p=>[p.latitude,p.longitude] as import('leaflet').LatLngTuple);if(line.length>1)L.polyline(line,{color:'#245f45',weight:4}).addTo(map);points.push(...line);marker(leg.to,`${i+1}. ${leg.to.label} · arrival ${leg.arrivalTime.slice(0,5)} · ${leg.route.minutes} min ${leg.mode.toLowerCase()}`);});
   if(points.length)map.fitBounds(L.latLngBounds(points),{padding:[20,20],maxZoom:15});
  });
  return ()=>{disposed=true;instance?.remove();};
 },[open,legs,origin]);
 if(!legs.length&&!origin)return null;
 return <div className="route-map"><button type="button" onClick={()=>setOpen(v=>!v)} aria-expanded={open}>{open?'Hide route map':'View route on map →'}</button>{open&&<div ref={container} className="route-map-canvas" role="region" aria-label="Planned route on OpenStreetMap"/>}</div>;
}
