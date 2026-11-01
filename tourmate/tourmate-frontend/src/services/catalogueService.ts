import {api} from '../api/client';
import type {Destination, Page, Place} from '../models';
export function getDestinations(query = '') {
 return api<Page<Destination>>(`/destinations${query ? `?${query}` : ''}`);
}
export function getDestination(id: string | undefined) {
 return api<Destination>(`/destinations/${id}`);
}
export function getPlaces(query = '') {
 return api<Page<Place>>(`/places${query ? `?${query}` : ''}`);
}
export function getPlace(id: string | undefined) {
 return api<Place>(`/places/${id}`);
}
