import {api} from '../api/client';
import type {Page} from '../models';
import type {Trip, TripInput, TripItemInput, TripSummary} from '../models/trip';
export function getTrips(page = 0) {return api<Page<TripSummary>>(`/trips?page=${page}&size=12`);}
export function getTrip(id: string) {return api<Trip>(`/trips/${encodeURIComponent(id)}`);}
export function createTrip(body: TripInput) {return api<Trip>('/trips', {method: 'POST', body: JSON.stringify(body)});}
export function updateTrip(id: number, body: TripInput) {return api<Trip>(`/trips/${id}`, {method: 'PUT', body: JSON.stringify(body)});}
export function deleteTrip(id: number) {return api<{deleted: boolean}>(`/trips/${id}`, {method: 'DELETE'});}
export function saveTripItem(id: number, body: TripItemInput, itemId?: number) {return api<Trip>(`/trips/${id}/items${itemId === undefined ? '' : `/${itemId}`}`, {method: itemId === undefined ? 'POST' : 'PUT', body: JSON.stringify(body)});}
export function deleteTripItem(id: number, itemId: number) {return api<Trip>(`/trips/${id}/items/${itemId}`, {method: 'DELETE'});}
export function reorderTripDay(id: number, dayId: number, itemIds: number[]) {return api<Trip>(`/trips/${id}/days/${dayId}/order`, {method: 'PUT', body: JSON.stringify({itemIds})});}
export function moveTripItem(id: number, itemId: number, dayId: number, position: number) {return api<Trip>(`/trips/${id}/items/${itemId}/move`, {method: 'PUT', body: JSON.stringify({dayId, position})});}
