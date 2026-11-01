import type {GeoPlace, GeoSearchParams, GeoResults, PlacePhoto} from '../models/geoapify';
import {api} from '../api/client';
export function searchGeoapify(search: GeoSearchParams, page = 0) {
 const params = new URLSearchParams({q: search.q, type: search.type, name: search.name, radius: String(search.radius), page: String(page)});
 if (search.lat !== undefined && search.lon !== undefined) {params.set('lat', String(search.lat)); params.set('lon', String(search.lon));}
 return api<GeoResults>(`/geoapify/search?${params}`, {cache: 'no-store'});
}
export function getGeoPlace(id: string, signal?: AbortSignal) {return api<GeoPlace>(`/geoapify/details/${encodeURIComponent(id)}`, {cache: 'no-store', signal});}
let activePhotos = 0;
const photoQueue: Array<() => void> = [];
export function getGeoPhoto(id: string, signal?: AbortSignal): Promise<PlacePhoto> {
 return new Promise((resolve, reject) => {
  const run = () => {
   if (signal?.aborted) {reject(new DOMException('Photo request cancelled', 'AbortError')); photoQueue.shift()?.(); return;}
   activePhotos++;
   api<PlacePhoto>(`/geoapify/photos/${encodeURIComponent(id)}`, {signal}).then(resolve, reject).finally(() => {
    activePhotos--; photoQueue.shift()?.();
   });
  };
  if (activePhotos < 3) run(); else photoQueue.push(run);
 });
}
