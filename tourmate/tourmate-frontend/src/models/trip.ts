export type ActivityCategory = 'HOTEL' | 'RESTAURANT' | 'ATTRACTION' | 'ACTIVITY' | 'TRANSPORT' | 'OTHER';
export interface SchedulePlace {title: string; category: ActivityCategory; placeSource: 'CUSTOM' | 'TOURMATE' | 'GEOAPIFY' | 'RAPIDAPI'; placeId: string | null; address: string | null; latitude: number | null; longitude: number | null}
export interface TripItemInput extends SchedulePlace {dayId: number; startTime: string; endTime: string; estimatedCost: number; notes: string}
export interface TripItem extends TripItemInput {id: number; position: number; mealType?: 'NONE' | 'BREAKFAST' | 'LUNCH' | 'DINNER'}
export interface TripDay {id: number; dayNumber: number; date: string; items: TripItem[]}
export interface TripInput {name: string; startDate: string; endDate: string; budget: number; travelers: number; destinationIds: number[]; status: 'DRAFT' | 'PLANNED' | 'COMPLETED'}
export interface TripSummary extends Omit<TripInput, 'destinationIds'> {id: number}
export interface Trip extends TripSummary {destinationIds: number[]; planningPreferences?: string | null; planningMode: string; estimatedTotal: number; remainingBudget: number; costByCategory: Record<string, number>; days: TripDay[]}
