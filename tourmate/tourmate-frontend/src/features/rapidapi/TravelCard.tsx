import {AddToTrip} from '../../components/AddToTrip';
import {travelSchedulePlace} from '../../utils/trip';
import {PlaceMap} from '../../components/PlaceMap';
import {Link} from 'react-router-dom';
import {TravelPhoto} from '../../components/TravelPhoto';
import {safeLink} from '../../utils/links';
import {getTravelMap} from '../../services/travelService';
import {ratingLabel, travelLink} from '../../utils/travel';
import {type TravelPlace} from '../../models/travel';
export function TravelCard({place}: {place: TravelPlace}) {
 return <article className="place-card"><Link to={travelLink(place)}>
  <div className="landscape small photo"><TravelPhoto src={safeLink(place.photoUrl)} alt={place.name}/></div>
  <div className="card-body"><h3>{place.name}</h3><p className="rating">{ratingLabel(place)}</p><p className="muted">{place.address || 'Address unavailable'}</p>{place.phone && <p>{place.phone}</p>}{place.priceText && <p className="price">{place.currency || 'Currency not supplied'} {place.priceText}{place.type === 'BOOKING_HOTEL' ? ' per stay' : ''}</p>}</div>
 </Link><div className="travel-source"><p>{place.provider}</p>{place.photoUrl && <p>{place.photoCredit}</p>}{safeLink(place.sourceUrl) && <a className="text-link" href={safeLink(place.sourceUrl)} target="_blank" rel="noreferrer">View provider listing ↗</a>}<AddToTrip place={travelSchedulePlace(place)}/><PlaceMap key={place.id} place={place} resolveLocation={() => getTravelMap(place)}/></div></article>;
}
