import {useEffect, useRef, useState} from 'react';
import {useQuery} from '@tanstack/react-query';
import {TravelPhoto} from '../../components/TravelPhoto';
import {getGeoPhoto} from '../../services/geoapifyService';
import {safeLink} from '../../utils/links';
import {type GeoPlace} from '../../models/geoapify';
export function GeoListingPhoto({place, large = false}: {place: GeoPlace; large?: boolean}) {
 const container = useRef<HTMLDivElement>(null);
 const [visible, setVisible] = useState(large);
 useEffect(() => {
  if (visible) return;
  if (!('IntersectionObserver' in window)) {setVisible(true); return;}
  const observer = new IntersectionObserver(entries => {
   if (entries.some(entry => entry.isIntersecting)) {setVisible(true); observer.disconnect();}
  }, {rootMargin: '150px'});
  if (container.current) observer.observe(container.current);
  return () => observer.disconnect();
 }, [visible]);
 const photo = useQuery({queryKey: ['geo-place-photo', place.id], queryFn: ({signal}) => getGeoPhoto(place.id, signal), enabled: visible,
  staleTime: 30 * 60 * 1000, gcTime: 30 * 60 * 1000, retry: false, refetchOnWindowFocus: false, refetchOnReconnect: false});
 const image = photo.data;
 const src = safeLink(image?.url);
 return <div className="geo-photo-panel" ref={container}>
  <div className={`landscape photo ${large ? 'detail-landscape' : 'small'}`}>
   {src ? <TravelPhoto src={src} alt={place.name} eager={large}/> : <div className="photo-unavailable" role="status">{!visible || photo.isFetching ? 'Finding a photo…' : photo.isError ? 'Photo could not load' : 'Photo unavailable'}</div>}
  </div>
  {photo.isError && <button className="photo-retry" onClick={() => void photo.refetch()}>Retry photo</button>}
  {src && image && <div className="geo-live-photo-credit"><span>{image.author}</span>{safeLink(image.licenseUrl) ? <a href={safeLink(image.licenseUrl)} target="_blank" rel="noreferrer">{image.license}</a> : <span>{image.license}</span>}{safeLink(image.sourceUrl) && <a href={safeLink(image.sourceUrl)} target="_blank" rel="noreferrer">Original photo & credits ↗</a>}</div>}
 </div>;
}
