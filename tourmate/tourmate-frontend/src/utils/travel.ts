import type {TravelPlace} from '../models/travel';
export function travelLink(place: TravelPlace) {return `/travel/${place.type}/${place.id}?${new URLSearchParams({resultToken: place.resultToken})}`;}

export function ratingLabel(place: TravelPlace) {
 return place.rating === null ? 'Rating unavailable' : `${place.rating.toFixed(1)} / ${place.ratingScale}${place.reviewCount === null ? '' : ` · ${place.reviewCount.toLocaleString()} reviews`}`;
}
