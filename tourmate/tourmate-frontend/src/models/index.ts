export interface Page<T> {content: T[]; totalElements: number; totalPages: number; number: number; size: number}
export interface Destination {id: number; name: string; city: string; country: string; description: string; coverImageUrl: string | null; latitude: number; longitude: number}
export type PlaceType = 'HOTEL' | 'RESTAURANT' | 'ATTRACTION' | 'ACTIVITY';
export interface Place {id: number; destinationId: number; name: string; type: PlaceType; description: string; address: string; latitude: number; longitude: number; averageRating: number; reviewCount: number; estimatedCost: number; priceLevel: number; coverImageUrl: string | null; website: string | null; phone: string | null}
export interface User {id: number; firstName: string; lastName: string; email: string; profileImageUrl: string | null; role: string}
export interface AuthResponse {accessToken: string; tokenType: string; expiresIn: number; user: User}
