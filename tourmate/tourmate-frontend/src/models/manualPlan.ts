import type {SchedulePlace, TripInput} from './trip';
export type MealType = 'NONE' | 'BREAKFAST' | 'LUNCH' | 'DINNER';
export interface PlanStop extends SchedulePlace {key: string; durationMinutes: number; preferredTime: string | null; estimatedCost: number; notes: string; mealType: MealType}
export interface PlanDay {date: string; area: string; startTime: string; endTime: string; stops: PlanStop[]}
export interface ManualPlanInput {trip: TripInput; startingLocation: string; transportMode: 'CAR' | 'PUBLIC_TRANSPORT' | 'WALKING' | 'CYCLING' | 'OTHER'; startingLatitude?:number; startingLongitude?:number; optimizeTravel?:boolean; transferMinutes: number; days: PlanDay[]}
export interface PlanPreview {optimization?:{enabled:boolean;beforeTravelMinutes:number;afterTravelMinutes:number;searchLimited:boolean;description:string};days: Array<{date: string; area: string; origin?:RoutePoint; travelLegs?:TravelLeg[]; visits: Array<{stop: PlanStop; startTime: string; endTime: string}>}>; unscheduled: Array<{date: string; title: string; reason: string}>; warnings: string[]; estimatedTotal: number; remainingBudget: number}
export interface RoutePoint {latitude:number;longitude:number;label:string}
export interface RouteEstimate {minutes:number;distanceMeters:number;geometry:RoutePoint[];elapsedSeconds:number[]}
export interface TravelLeg {from:RoutePoint;to:RoutePoint;departureTime:string;arrivalTime:string;mode:string;route:RouteEstimate}
export interface PlanNearbyResponse {anchor:RoutePoint;at:string;context:string;mode:string;options:Array<{place:import('./geoapify').GeoPlace;travelMinutes:number;distanceMeters:number}>;warnings:string[]}

export interface PlanMealOptionsResponse {meals:Array<{key:string;mealType:MealType;at:string|null;suggestions:PlanNearbyResponse|null;error:string|null}>}
