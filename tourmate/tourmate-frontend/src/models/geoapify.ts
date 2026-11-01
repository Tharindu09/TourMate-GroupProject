export interface GeoPlace {
 id: string; name: string; address: string; latitude: number; longitude: number; categories: string[];
 website: string | null; phone: string | null; openingHours: string | null; description: string | null; wheelchair: string | null;
}
export interface GeoSearchParams {q: string; type: string; name: string; radius: number; lat?: number; lon?: number}
export interface GeoResults {places: GeoPlace[]; page: number; hasMore: boolean; latitude: number; longitude: number; location: string}
export interface PlacePhoto {url: string | null; sourceUrl: string | null; author: string | null; license: string | null; licenseUrl: string | null}
