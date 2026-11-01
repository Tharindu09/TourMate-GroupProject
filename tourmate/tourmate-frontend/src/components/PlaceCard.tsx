import {Link} from 'react-router-dom';
import type {Place} from '../types';
import {category, money, scenery} from '../utils/format';
export function PlaceCard({place}: {place: Place}) {
  return <Link className="place-card" to={`/places/${place.id}`}>
    <div className={`landscape small ${scenery(place.destinationId)}`}><span className="card-tag">{category(place.type)}</span><span className="terrain" aria-hidden="true"/></div>
    <div className="card-body"><h3>{place.name}</h3><p className="rating">{place.reviewCount > 0 ? `● ${place.averageRating.toFixed(1)} · ${place.reviewCount} reviews` : 'New to TourMate'}</p><p className="muted">{place.address}</p><p className="price">{Number(place.estimatedCost) === 0 ? 'Free to explore' : `${money(place.estimatedCost)} estimated`}</p></div>
  </Link>;
}
