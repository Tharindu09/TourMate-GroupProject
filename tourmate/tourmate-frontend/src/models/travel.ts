export interface TravelReview {title: string | null; text: string; author: string | null; rating: number | null; date: string | null; url: string | null}
export interface TravelPlace {
 id: string; type: string; provider: string; name: string; description: string | null; address: string | null;
 latitude: number | null; longitude: number | null; photoUrl: string | null; photoCredit: string | null;
 rating: number | null; ratingScale: number; reviewCount: number | null; phone: string | null; website: string | null; sourceUrl: string | null;
 openingHours: string[]; amenities: string[]; reviews: TravelReview[]; priceText: string | null; currency: string | null; resultToken: string;
}
export interface TravelSearch {q: string; type: string; radius: number; lat?: number; lon?: number; checkin: string; checkout: string; rooms: number; adults: number}
export interface TravelResults {places: TravelPlace[]; page: number; hasMore: boolean; latitude: number; longitude: number; location: string}
