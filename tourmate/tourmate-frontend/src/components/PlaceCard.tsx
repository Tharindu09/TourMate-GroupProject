import {AddToTrip} from '../components/AddToTrip';
import {localSchedulePlace} from '../utils/trip';
import {PlaceMap} from './PlaceMap';
import {Link} from 'react-router-dom';
import type {Place} from '../models';
import {category, money} from '../utils/format';
import {TravelPhoto, placePhoto} from './TravelPhoto';
export function PlaceCard({place}: {place: Place}) {
  return <article className="place-card"><Link to={`/places/${place.id}`}>
    <div className="landscape small photo"><TravelPhoto src={place.coverImageUrl} photoKey={placePhoto(place.name)} alt={place.name.includes('(demo)') ? 'Ella scenery, illustrative image for this demo listing' : place.name}/><span className="card-tag">{category(place.type)}</span>{place.name.includes('(demo)') && <span className="demo-photo-label">Ella scenery · demo listing</span>}</div>
    <div className="card-body"><h3>{place.name}</h3><p className="rating">{place.reviewCount > 0 ? `● ${place.averageRating.toFixed(1)} · ${place.reviewCount} reviews` : 'New to TourMate'}</p><p className="muted">{place.address}</p><p className="price">{Number(place.estimatedCost) === 0 ? 'Free to explore' : `${money(place.estimatedCost)} estimated`}</p></div>
  </Link><div className="geo-card-meta"><AddToTrip place={localSchedulePlace(place)}/><PlaceMap place={place}/></div></article>;
}
