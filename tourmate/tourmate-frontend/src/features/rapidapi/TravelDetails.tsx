import {AddToTrip} from '../../components/AddToTrip';
import {travelSchedulePlace} from '../../utils/trip';
import {PlaceMap} from '../../components/PlaceMap';
import {useEffect, useState} from 'react';
import {Link, useParams, useSearchParams} from 'react-router-dom';
import {getTravelMap, getTravelDetails} from '../../services/travelService';
import {ratingLabel} from '../../utils/travel';
import {type TravelPlace} from '../../models/travel';
import {safeLink} from '../../utils/links';
import {TravelPhoto} from '../../components/TravelPhoto';
import {Failure, Loading} from '../../components/Status';
export function TravelDetails() {
 const {type = '', id = ''} = useParams(); const [params] = useSearchParams(); const token = params.get('resultToken') || '';
 const [place, setPlace] = useState<TravelPlace | null>(null); const [error, setError] = useState<Error | null>(null); const [attempt, setAttempt] = useState(0);
 useEffect(() => {
  const controller = new AbortController(); setPlace(null); setError(null);
  getTravelDetails(type, id, token, controller.signal).then(data => {if (!controller.signal.aborted) setPlace(data);}).catch(e => {if (!controller.signal.aborted) setError(e instanceof Error ? e : new Error('Could not load place'));});
  return () => controller.abort();
 }, [type, id, token, attempt]);
 if (error) return <section className="page"><Link to="/search">← Search travel places</Link><Failure error={error} retry={() => setAttempt(a => a + 1)}/></section>;
 if (!place) return <Loading/>;
 return <section className="page travel-detail"><Link to="/search">← Search travel places</Link><p className="eyebrow detail-label">{place.provider}</p><h1>{place.name}</h1><p className="rating">{ratingLabel(place)}</p><p className="intro">{place.address || 'Address unavailable'}</p>
 <div className="detail-landscape landscape photo"><TravelPhoto src={safeLink(place.photoUrl)} alt={place.name} eager/></div>{place.photoUrl && <p className="travel-note muted">{place.photoCredit}</p>}
 <div className="detail-columns"><article><h2>About</h2><p className="intro">{place.description || 'The provider has not supplied a description.'}</p><h2>Location</h2><AddToTrip place={travelSchedulePlace(place)}/><PlaceMap autoShow key={place.id} place={place} resolveLocation={() => getTravelMap(place)}/><h2>Opening hours</h2>{place.openingHours.length ? <ul className="travel-hours">{place.openingHours.map((hour, i) => <li key={i}>{hour}</li>)}</ul> : <p>Opening hours unavailable.</p>}
 {place.amenities.length > 0 && <><h2>Amenities</h2><ul>{place.amenities.map((item, i) => <li key={i}>{item}</li>)}</ul></>}
 {safeLink(place.sourceUrl) && <p><a className="text-link" href={safeLink(place.sourceUrl)} target="_blank" rel="noreferrer">View original provider listing ↗</a></p>}</article>
 <aside><h3>Contact</h3><p>{place.phone || 'Phone unavailable'}</p>{safeLink(place.website) ? <p><a className="text-link" href={safeLink(place.website)} target="_blank" rel="noreferrer">Visit website ↗</a></p> : <p>Website unavailable</p>}{place.priceText && <p>{place.type === 'BOOKING_HOTEL' ? 'Stay total' : 'Provider price'}: {place.currency || 'Currency not supplied'} {place.priceText}</p>}<small>Confirm current hours, prices and availability directly with the venue.</small></aside></div>
 <section className="travel-reviews"><h2>Reviews</h2>{place.reviews.length ? place.reviews.map((review, i) => <article className="travel-review" key={i}><h3>{review.title}</h3><p className="muted">{review.author || 'Provider reviewer'}{review.rating !== null ? ` · ${review.rating} / 5` : ''}{review.date ? ` · ${review.date}` : ''}</p><p>{review.text}</p>{safeLink(review.url) && <a className="text-link" href={safeLink(review.url)} target="_blank" rel="noreferrer">Original review ↗</a>}</article>) : <p>The provider supplied no review text for this response.{place.reviewCount !== null ? ` Its reported review count is ${place.reviewCount.toLocaleString()}.` : ''}</p>}</section>
 <p className="muted travel-note">Data supplied by {place.provider}. These are external listings; missing fields are not estimated or generated.{place.type === 'BOOKING_HOTEL' ? ' This page shows the availability search snapshot, which expires after 15 minutes; search again for refreshed dates and prices.' : ''}</p>
 </section>;
}
