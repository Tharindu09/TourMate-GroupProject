import {AddToTrip} from '../../components/AddToTrip';
import {geoSchedulePlace} from '../../utils/trip';
import {PlaceMap} from '../../components/PlaceMap';
import {useEffect, useState} from 'react';
import {Link, useParams} from 'react-router-dom';
import {getGeoPlace} from '../../services/geoapifyService';
import {safeLink} from '../../utils/links';
import {type GeoPlace} from '../../models/geoapify';
import {GeoListingPhoto} from './GeoListingPhoto';
import {GeoAttribution} from './GeoContent';
import {Failure, Loading} from '../../components/Status';
export function GeoPlaceDetails() {
 const {id = ''} = useParams(); const [place, setPlace] = useState<GeoPlace | null>(null); const [error, setError] = useState<Error | null>(null);
 useEffect(() => {
  const controller = new AbortController(); setPlace(null); setError(null);
  getGeoPlace(id, controller.signal).then(data => {if (!controller.signal.aborted) setPlace(data);}).catch(e => {if (!controller.signal.aborted) setError(e instanceof Error ? e : new Error('Could not load place'));});
  return () => controller.abort();
 }, [id]);
 if (error) return <section className="page"><Link to="/search">← Search places</Link><Failure error={error}/></section>;
 if (!place) return <Loading/>;
 return <section className="page geo-detail"><Link to="/search">← Search places</Link><p className="eyebrow detail-label">{place.categories[0]?.replaceAll('.', ' › ').replaceAll('_', ' ') || 'Place'}</p><h1>{place.name}</h1><GeoAttribution/><p className="intro">{place.address}</p>
 <GeoListingPhoto place={place} large/><div className="detail-columns"><article><h2>Plan your visit</h2>{place.description && <p className="intro">{place.description}</p>}<AddToTrip place={geoSchedulePlace(place)}/><PlaceMap autoShow key={place.id} place={place}/><p className="muted">Latitude {place.latitude.toFixed(5)} · Longitude {place.longitude.toFixed(5)}</p><h2>Opening hours</h2><p className="geo-opening-hours">{place.openingHours || 'Opening hours unavailable.'}</p>{place.openingHours && <p className="muted">Hours are shown in the format recorded by the data source. Confirm with the venue before visiting.</p>}</article>
 <aside><h3>Contact</h3><p>{place.phone || 'Phone unavailable'}</p>{safeLink(place.website) ? <p><a className="text-link" href={safeLink(place.website)} target="_blank" rel="noreferrer">Visit website ↗</a></p> : <p>Website unavailable</p>}<small>Contact the venue for current prices and availability.</small></aside></div>
 <div className="geo-data-note"><h3>About this listing</h3><p>Geoapify supplies location data from OpenStreetMap. Customer reviews and ratings are not provided by this integration. Photographs are retrieved from the media references attached to this place, when available, with source and license credits. TourMate’s own review feature is planned.</p></div><GeoAttribution/>
 </section>;
}
