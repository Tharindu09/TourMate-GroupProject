// @vitest-environment jsdom
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {cleanup, fireEvent, render, screen, waitFor, within} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {TripsPage} from './TripsPage';
import {TripDetails} from './TripDetails';
import {useAuth} from '../auth/AuthContext';
import * as service from '../../services/tripService';
import {getDestinations, getPlaces} from '../../services/catalogueService';
import type {Trip, TripItem} from '../../models/trip';
vi.mock('../auth/AuthContext',()=>({useAuth:vi.fn()}));
vi.mock('../../services/tripService',()=>({getTrips:vi.fn(),getTrip:vi.fn(),createTrip:vi.fn(),updateTrip:vi.fn(),deleteTrip:vi.fn(),saveTripItem:vi.fn(),deleteTripItem:vi.fn(),moveTripItem:vi.fn(),reorderTripDay:vi.fn()}));
vi.mock('../../services/catalogueService',()=>({getDestinations:vi.fn(),getPlaces:vi.fn()}));
const user={id:7,firstName:'Test',lastName:'User',email:'test@example.com',profileImageUrl:null,role:'USER'};
const trip:Trip={id:1,name:'Ella adventure',startDate:'2027-01-01',endDate:'2027-01-02',budget:10000,travelers:2,status:'DRAFT',planningMode:'MANUAL',destinationIds:[],estimatedTotal:0,remainingBudget:10000,costByCategory:{},days:[{id:11,dayNumber:1,date:'2027-01-01',items:[]},{id:12,dayNumber:2,date:'2027-01-02',items:[]}]};
const item:TripItem={id:21,dayId:11,title:'Morning walk',category:'ACTIVITY',placeSource:'CUSTOM',placeId:null,address:null,latitude:null,longitude:null,startTime:'09:00:00',endTime:'10:00:00',estimatedCost:500,notes:'Bring water',position:0};
function view(path='/trips/1',state?:object) {
 const client=new QueryClient({defaultOptions:{queries:{retry:false},mutations:{retry:false}}});
 return render(<QueryClientProvider client={client}><MemoryRouter initialEntries={[{pathname:path,state}]}><Routes><Route path="/trips" element={<TripsPage/>}/><Route path="/trips/:id" element={<TripDetails/>}/></Routes></MemoryRouter></QueryClientProvider>);
}
beforeEach(()=>{
 vi.mocked(useAuth).mockReturnValue({user,authenticate:vi.fn(),logout:vi.fn(),updateUser:vi.fn()});
 vi.mocked(service.getTrip).mockResolvedValue(structuredClone(trip));
 vi.mocked(service.getTrips).mockResolvedValue({content:[],totalElements:0,totalPages:0,number:0,size:12});
 vi.mocked(getDestinations).mockResolvedValue({content:[],totalElements:0,totalPages:0,number:0,size:100});
 vi.mocked(getPlaces).mockResolvedValue({content:[],totalElements:0,totalPages:0,number:0,size:12});
});
afterEach(()=>{cleanup();vi.resetAllMocks();});
describe('manual trip scheduler',()=>{
 it('does not load private trips when signed out',()=>{
  vi.mocked(useAuth).mockReturnValue({user:null,authenticate:vi.fn(),logout:vi.fn(),updateUser:vi.fn()});
  view('/trips');expect(screen.getByRole('link',{name:'Sign in to plan →'})).toBeTruthy();expect(service.getTrips).not.toHaveBeenCalled();
 });
 it('creates a trip and opens the day schedule',async()=>{
  vi.mocked(service.createTrip).mockResolvedValue(trip);view('/trips');
  fireEvent.click(screen.getByRole('button',{name:'Create empty trip'}));
  fireEvent.change(screen.getByLabelText('Trip name'),{target:{value:'Ella adventure'}});
  fireEvent.change(screen.getByLabelText('Start date'),{target:{value:'2027-01-01'}});
  fireEvent.change(screen.getByLabelText('End date'),{target:{value:'2027-01-02'}});
  fireEvent.change(screen.getByLabelText('Total budget (LKR)'),{target:{value:'10000'}});
  fireEvent.click(screen.getByRole('button',{name:'Create trip →'}));
  expect(await screen.findByRole('heading',{name:'Ella adventure'})).toBeTruthy();
  expect(service.createTrip).toHaveBeenCalledWith(expect.objectContaining({name:'Ella adventure',startDate:'2027-01-01',endDate:'2027-01-02',budget:10000}));
  expect(screen.getAllByRole('button',{name:'+ Add activity'})).toHaveLength(2);
 });
 it('saves a custom activity and shows the saved timeline and budget',async()=>{
  vi.mocked(service.saveTripItem).mockResolvedValue({...trip,estimatedTotal:500,remainingBudget:9500,costByCategory:{ACTIVITY:500},days:[{...trip.days[0],items:[item]},trip.days[1]]});
  view();await screen.findByRole('heading',{name:'Ella adventure'});
  fireEvent.click(screen.getAllByRole('button',{name:'+ Add activity'})[0]);
  fireEvent.change(screen.getByLabelText('Activity name'),{target:{value:'Morning walk'}});
  fireEvent.change(screen.getByLabelText('Estimated cost (LKR)'),{target:{value:'500'}});
  fireEvent.change(screen.getByLabelText('Notes'),{target:{value:'Bring water'}});
  fireEvent.click(screen.getByRole('button',{name:'Save activity'}));
  expect(await screen.findByRole('heading',{name:'Morning walk'})).toBeTruthy();
  expect(service.saveTripItem).toHaveBeenCalledWith(1,expect.objectContaining({dayId:11,title:'Morning walk',estimatedCost:500,notes:'Bring water',placeSource:'CUSTOM'}),undefined);
  expect(screen.queryByLabelText('Activity name')).toBeNull();expect(screen.getByText('Bring water')).toBeTruthy();
 });
 it('keeps unsaved input and the existing schedule when the backend rejects an overlap',async()=>{
  vi.mocked(service.saveTripItem).mockRejectedValue(new Error('This time overlaps with Morning walk. Choose another time.'));
  view();await screen.findByRole('heading',{name:'Ella adventure'});
  fireEvent.click(screen.getAllByRole('button',{name:'+ Add activity'})[0]);fireEvent.change(screen.getByLabelText('Activity name'),{target:{value:'Cafe'}});
  fireEvent.click(screen.getByRole('button',{name:'Save activity'}));
  expect(await screen.findByRole('alert')).toBeTruthy();expect((screen.getByLabelText('Activity name') as HTMLInputElement).value).toBe('Cafe');
 });
 it('prefills a discovered place without copying foreign-currency prices',async()=>{
  view('/trips/1',{place:{title:'Temple',category:'ATTRACTION',placeSource:'RAPIDAPI',placeId:'123',address:null,latitude:null,longitude:null}});
  await screen.findByLabelText('Activity name');
  expect((screen.getByLabelText('Activity name') as HTMLInputElement).value).toBe('Temple');expect((screen.getByLabelText('Estimated cost (LKR)') as HTMLInputElement).value).toBe('0');
 });
 it('supports keyboard reordering and moving through the edit form',async()=>{
  const second={...item,id:22,title:'Cafe',startTime:'11:00:00',endTime:'12:00:00',position:1};
  const full={...trip,days:[{...trip.days[0],items:[item,second]},trip.days[1]]};
  vi.mocked(service.getTrip).mockResolvedValue(full);vi.mocked(service.reorderTripDay).mockResolvedValue({...full,days:[{...trip.days[0],items:[second,item]},trip.days[1]]});
  vi.mocked(service.saveTripItem).mockResolvedValue({...trip,days:[{...trip.days[0],items:[second]},{...trip.days[1],items:[{...item,dayId:12}]}]});
  view();await screen.findByRole('heading',{name:'Morning walk'});
  fireEvent.click(screen.getByRole('button',{name:'Move Morning walk down'}));await waitFor(()=>expect(service.reorderTripDay).toHaveBeenCalledWith(1,11,[22,21]));
  await screen.findByText('Schedule saved.');fireEvent.click(screen.getByRole('button',{name:'Edit Morning walk'}));
  fireEvent.change(screen.getByLabelText('Day'),{target:{value:'12'}});fireEvent.click(screen.getByRole('button',{name:'Save activity'}));
  await waitFor(()=>expect(service.saveTripItem).toHaveBeenCalledWith(1,expect.objectContaining({dayId:12}),21));
  expect(await within(screen.getByRole('region',{name:'Day 2'})).findByRole('heading',{name:'Morning walk'})).toBeTruthy();
 });
 it('saves a native drag to another day with the original activity identity',async()=>{
  const full={...trip,days:[{...trip.days[0],items:[item]},trip.days[1]]};
  vi.mocked(service.getTrip).mockResolvedValue(full);
  vi.mocked(service.moveTripItem).mockResolvedValue({...trip,days:[trip.days[0],{...trip.days[1],items:[{...item,dayId:12}]}]});
  view();await screen.findByRole('heading',{name:'Morning walk'});
  fireEvent.dragStart(screen.getByLabelText('Drag Morning walk'),{dataTransfer:{setData:vi.fn(),effectAllowed:'move'}});
  fireEvent.dragOver(screen.getByRole('region',{name:'Day 2'}));
  fireEvent.drop(screen.getByRole('region',{name:'Day 2'}));
  await waitFor(()=>expect(service.moveTripItem).toHaveBeenCalledWith(1,21,12,0));
  expect(await within(screen.getByRole('region',{name:'Day 2'})).findByRole('heading',{name:'Morning walk'})).toBeTruthy();
 });
 it('requires an explicit confirmation before deleting a whole trip',async()=>{
  vi.mocked(service.deleteTrip).mockResolvedValue({deleted:true});view();await screen.findByRole('heading',{name:'Ella adventure'});
  fireEvent.click(screen.getByRole('button',{name:'Delete trip'}));expect(service.deleteTrip).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button',{name:'Delete permanently'}));await waitFor(()=>expect(service.deleteTrip).toHaveBeenCalledWith(1));
  expect(await screen.findByRole('heading',{name:'Your trips.'})).toBeTruthy();
 });
});
