import {AddToTrip} from '../components/AddToTrip';
import {localSchedulePlace} from '../utils/trip';
import {PlaceMap} from '../components/PlaceMap';
import {Link, useParams} from 'react-router-dom';
import {useQuery} from '@tanstack/react-query';
import {getDestination, getPlace, getPlaces} from '../services/catalogueService';

import {Loading, Failure} from '../components/Status';
import {PlaceCard} from '../components/PlaceCard';
import {category, money} from '../utils/format';
import {TravelPhoto, destinationPhoto, placePhoto} from '../components/TravelPhoto';
export function DestinationDetails() {
 const {id} = useParams();
 const destination = useQuery({queryKey: ['destination', id], queryFn: () => getDestination(id)});
 const places = useQuery({queryKey: ['destinationPlaces', id], queryFn: () => getPlaces(`destinationId=${id}&size=100`)});
 if (destination.isPending) return <Loading/>; if (destination.isError) return <Failure error={destination.error}/>;
 const d = destination.data;
 return <section className="page"><Link to="/search">← Explore Sri Lanka</Link><div className="detail-landscape landscape photo"><TravelPhoto src={d.coverImageUrl} photoKey={destinationPhoto(d.name)} alt={d.name + ", Sri Lanka"} eager/><div><p className="eyebrow">{d.country}</p><h1>{d.name}</h1></div></div><p className="intro">{d.description}</p><div className="section-heading"><h2>Get to know {d.name}</h2><Link to={`/search?destinationId=${d.id}&location=${encodeURIComponent(d.name)}`}>Filter places ↗</Link></div>{places.isPending ? <Loading/> : places.isError ? <Failure error={places.error}/> : <div className="place-grid">{places.data.content.map(p => <PlaceCard key={p.id} place={p}/>)}</div>}</section>;
}
export function PlaceDetails() {
 const {id} = useParams(); const place = useQuery({queryKey: ['place', id], queryFn: () => getPlace(id)});
 if (place.isPending) return <Loading/>; if (place.isError) return <Failure error={place.error}/>;
 const p = place.data;
 return <section className="page"><Link to={`/destinations/${p.destinationId}`}>← Back to destination</Link><p className="eyebrow detail-label">{category(p.type)}</p><h1>{p.name}</h1><p className="muted">{p.address}</p><div className="detail-landscape landscape photo"><TravelPhoto src={p.coverImageUrl} photoKey={placePhoto(p.name)} alt={p.name.includes('(demo)') ? 'Ella scenery, illustrative image for this demo listing' : p.name} eager/>{p.name.includes('(demo)') && <span className="demo-photo-label">Ella scenery · demo listing</span>}</div><div className="detail-columns"><article><h2>About this place</h2><p className="intro">{p.description}</p><h2>Location</h2><p>{p.address}</p><AddToTrip place={localSchedulePlace(p)}/><PlaceMap autoShow key={p.id} place={p}/></article><aside><h3>Good to know</h3><p>{p.reviewCount ? `${p.averageRating} / 5 · ${p.reviewCount} reviews` : 'No reviews yet'}</p><p>{p.estimatedCost ? `${money(p.estimatedCost)} estimated` : 'Free to explore'}</p><small>Sample listing. Confirm prices, access and opening times before visiting.</small></aside></div></section>;
}
