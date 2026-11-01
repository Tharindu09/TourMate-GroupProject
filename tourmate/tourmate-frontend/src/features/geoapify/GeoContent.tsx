import {AddToTrip} from '../../components/AddToTrip';
import {geoSchedulePlace} from '../../utils/trip';
import {PlaceMap} from '../../components/PlaceMap';
import {Link} from 'react-router-dom';
import {type GeoPlace} from '../../models/geoapify';
import {GeoListingPhoto} from './GeoListingPhoto';
export function GeoAttribution() {
 return <p className="geo-attribution">Powered by <a href="https://www.geoapify.com/" target="_blank" rel="noreferrer">Geoapify</a> · © <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noreferrer">OpenStreetMap contributors</a></p>;
}
export function GeoPlaceCard({place}: {place: GeoPlace}) {
 return <article className="place-card geo-place-card"><GeoListingPhoto place={place}/><Link className="geo-card-link" to={`/geoapify/places/${encodeURIComponent(place.id)}`}>
 <div className="card-body"><p className="geo-category">{place.categories[0]?.replaceAll('.', ' › ').replaceAll('_', ' ') || 'Place'}</p><h3>{place.name}</h3><p className="muted">{place.address}</p><span className="text-link">View details →</span></div></Link>
 <div className="geo-card-meta"><AddToTrip place={geoSchedulePlace(place)}/><PlaceMap place={place}/></div></article>;
}
