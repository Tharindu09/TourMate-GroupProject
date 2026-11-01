import type {TravelPlace, TravelSearch, TravelResults} from '../models/travel';
import {api} from '../api/client';
export function searchTravel(search: TravelSearch, page = 0) {
 const params = new URLSearchParams({q: search.q, type: search.type, radius: String(search.radius), page: String(page), rooms: String(search.rooms), adults: String(search.adults)});
 if (search.type.endsWith('HOTEL')) {params.set('checkin', search.checkin); params.set('checkout', search.checkout);}
 if (search.lat !== undefined && search.lon !== undefined) {params.set('lat', String(search.lat)); params.set('lon', String(search.lon));}
 return api<TravelResults>(`/rapidapi/search?${params}`, {cache: 'no-store'});
}
export function getTravelDetails(type: string, id: string, token: string, signal?: AbortSignal) {
 return api<TravelPlace>(`/rapidapi/details/${encodeURIComponent(type)}/${encodeURIComponent(id)}?${new URLSearchParams({resultToken: token})}`, {cache: 'no-store', signal});
}


export function getTravelMap(place: TravelPlace) {
 return api<{latitude: number; longitude: number}>(`/rapidapi/map/${encodeURIComponent(place.type)}/${encodeURIComponent(place.id)}?${new URLSearchParams({resultToken: place.resultToken})}`, {cache: 'no-store'});
}
