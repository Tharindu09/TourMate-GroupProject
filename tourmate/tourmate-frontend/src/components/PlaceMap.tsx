import {useEffect, useId, useRef, useState} from 'react';

type MapPlace = {name: string; address?: string | null; latitude?: number | null; longitude?: number | null};
export function mapLinks(place: MapPlace) {
 const {latitude: lat, longitude: lon} = place;
 if (typeof lat !== 'number' || typeof lon !== 'number' || !Number.isFinite(lat) || !Number.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180) {
  const query = [place.name, place.address].filter(Boolean).join(', ');
  return {url: `https://www.openstreetmap.org/search?${new URLSearchParams({query})}`, embed: null};
 }
 const params = new URLSearchParams({bbox: [Math.max(-180, lon - .008), Math.max(-90, lat - .006), Math.min(180, lon + .008), Math.min(90, lat + .006)].join(','), layer: 'mapnik', marker: `${lat},${lon}`});
 return {url: `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lon}#map=16/${lat}/${lon}`, embed: `https://www.openstreetmap.org/export/embed.html?${params}`};
}
export function PlaceMap({place, resolveLocation, autoShow = false}: {place: MapPlace; resolveLocation?: () => Promise<{latitude: number; longitude: number}>; autoShow?: boolean}) {
 const [open, setOpen] = useState(autoShow); const id = useId();
 const [coordinates, setCoordinates] = useState<{latitude: number; longitude: number} | null>(null);
 const [busy, setBusy] = useState(false); const [error, setError] = useState<string | null>(null);
 const resolver = useRef(resolveLocation); resolver.current = resolveLocation;
 const initialRequest = useRef<Promise<{latitude: number; longitude: number}> | null>(null);
 useEffect(() => {
  if (!autoShow || mapLinks(place).embed || !resolver.current) return;
  let active = true;
  setBusy(true); setError(null);
  initialRequest.current ??= Promise.resolve().then(() => resolver.current!());
  initialRequest.current.then(location => {
   if (!active) return;
   if (!mapLinks({...place, ...location}).embed) {setError('Exact coordinates are unavailable for this place.'); setBusy(false); return;}
   setCoordinates(location); setOpen(true); setBusy(false);
  }, e => {if (active) {setError(e instanceof Error ? e.message : 'Could not load the location.'); setBusy(false);}});
  return () => {active = false;};
 }, [autoShow, place.name, place.latitude, place.longitude]);
 const {url, embed} = mapLinks({...place, ...coordinates});
 async function toggle() {
  if (busy) return;
  if (embed) {setOpen(value => !value); return;}
  if (!resolveLocation) return;
  setBusy(true); setError(null);
  try {
   const location = await resolveLocation();
   if (!mapLinks({...place, ...location}).embed) throw new Error('Exact coordinates are unavailable for this place.');
   setCoordinates(location); setOpen(true);
  } catch (e) {setError(e instanceof Error ? e.message : 'Could not load the location. Please try again.');}
  finally {setBusy(false);}
 }
 return <div className="place-map">
  {(!autoShow || error) && (embed || resolveLocation) ? <button type="button" className="map-toggle" disabled={busy} aria-expanded={open} aria-controls={id} onClick={() => void toggle()}>{busy ? 'Finding location…' : open ? 'Hide map ↑' : 'View on map →'}</button>
   : !embed && !resolveLocation ? <><a className="text-link" href={url} target="_blank" rel="noopener noreferrer">View on map ↗</a><p className="map-note">Search OpenStreetMap by name; exact coordinates unavailable.</p></> : null}
  {autoShow && busy && <p className="map-note" role="status">Loading map location…</p>}
  {error && <div role="alert" className="map-note"><p>{error}</p><a className="text-link" href={url} target="_blank" rel="noopener noreferrer">Search OpenStreetMap by name ↗</a></div>}
  {embed && open && <div id={id} className="map-panel"><iframe title={`OpenStreetMap location of ${place.name}`} src={embed} loading="lazy" referrerPolicy="no-referrer" allowFullScreen/>
   <div className="map-caption"><a className="text-link" href={url} target="_blank" rel="noopener noreferrer">Open larger map ↗</a><span>© <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap contributors</a></span></div></div>}
 </div>;
}
