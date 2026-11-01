import type {GeoPlace} from '../models/geoapify';
export function safeLink(value?: string | null) {
 if (!value) return undefined;
 try {const url = new URL(value); return url.protocol === 'https:' || url.protocol === 'http:' ? url.href : undefined;} catch {return undefined;}
}
export function mapUrl(place: GeoPlace) {return `https://www.openstreetmap.org/?mlat=${place.latitude}&mlon=${place.longitude}#map=16/${place.latitude}/${place.longitude}`;}
