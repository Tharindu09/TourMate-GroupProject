import {useState} from 'react';
import photos from '../data/photos.json';

type PhotoKey = keyof typeof photos;
const destinationKeys: Record<string, PhotoKey> = {
  Ella: 'ella', Kandy: 'kandy', Galle: 'galle-fort', Sigiriya: 'sigiriya',
  'Nuwara Eliya': 'nuwara-eliya', Mirissa: 'mirissa', Colombo: 'colombo',
};
const placeKeys: Record<string, PhotoKey> = {
  'Nine Arch Bridge': 'nine-arch-bridge', 'Little Adam’s Peak': 'little-adams-peak',
  'Ravana Falls': 'ravana-falls', 'Kandy Lake': 'kandy-lake', 'Galle Fort': 'galle-fort',
  'Sigiriya Rock Fortress': 'sigiriya', 'Victoria Park': 'victoria-park', 'Mirissa Beach': 'mirissa',
  'Hill Country Guesthouse (demo)': 'ella', 'Tea Garden Kitchen (demo)': 'ella',
};
export function destinationPhoto(name: string) {return destinationKeys[name];}
export function placePhoto(name: string) {return placeKeys[name];}

export function TravelPhoto({src, photoKey, alt, eager = false}: {
  src?: string | null; photoKey?: PhotoKey; alt: string; eager?: boolean;
}) {
  const fallback = photoKey ? photos[photoKey] : undefined;
  const [failedSources, setFailedSources] = useState<string[]>([]);
  const active = [src, fallback?.src].find((value): value is string => !!value && !failedSources.includes(value));
  if (!active) return <span className="photo-unavailable">Photo unavailable</span>;
  return <img className="travel-photo" src={active} alt={alt} loading={eager ? 'eager' : 'lazy'}
    decoding="async" onError={() => setFailedSources(previous => [...previous, active])}/>;
}
