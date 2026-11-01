// @vitest-environment jsdom
import {afterEach,beforeEach,describe,expect,it,vi} from 'vitest';
import {cleanup,fireEvent,render,screen,waitFor,within} from '@testing-library/react';
import {QueryClient,QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter,Route,Routes} from 'react-router-dom';
import {GuidedTripPlanner} from './GuidedTripPlanner';
import {useAuth} from '../auth/AuthContext';
import {searchTravel} from '../../services/travelService';
import {searchGeoapify} from '../../services/geoapifyService';
import {getDestinations,getPlaces} from '../../services/catalogueService';
import {previewManualPlan,saveManualPlan,getAutomaticMealOptions,getRouteNearby} from '../../services/manualPlanService';
import type {TravelPlace} from '../../models/travel';
import type {Trip} from '../../models/trip';
vi.mock('../auth/AuthContext',()=>({useAuth:vi.fn()}));
vi.mock('../../services/travelService',()=>({searchTravel:vi.fn()}));
vi.mock('../../services/geoapifyService',()=>({searchGeoapify:vi.fn()}));
vi.mock('../../services/catalogueService',()=>({getDestinations:vi.fn(),getPlaces:vi.fn()}));
vi.mock('../../services/manualPlanService',()=>({previewManualPlan:vi.fn(),saveManualPlan:vi.fn(),getAutomaticMealOptions:vi.fn(),getRouteNearby:vi.fn()}));
const place=(type:string):TravelPlace=>({id:type==='RESTAURANT'?'202':'101',type,name:type==='RESTAURANT'?'Lunch cafe':'Nine Arch Bridge',provider:'Travel Advisor',description:null,address:'Ella',latitude:null,longitude:null,photoUrl:null,photoCredit:null,rating:4.5,ratingScale:5,reviewCount:10,phone:null,website:null,sourceUrl:null,openingHours:[],amenities:[],reviews:[],priceText:'USD 50',currency:'USD',resultToken:'test'});
function view(){const client=new QueryClient({defaultOptions:{queries:{retry:false}}});return render(<QueryClientProvider client={client}><MemoryRouter initialEntries={['/trips/plan']}><Routes><Route path="/trips/plan" element={<GuidedTripPlanner/>}/><Route path="/trips/:id" element={<p>Saved itinerary</p>}/></Routes></MemoryRouter></QueryClientProvider>);}
function basics(){fireEvent.change(screen.getByLabelText('Starting location'),{target:{value:'Colombo hotel'}});fireEvent.change(screen.getByLabelText('Main areas to visit'),{target:{value:'Ella'}});fireEvent.change(screen.getByLabelText('Trip name'),{target:{value:'My selected trip'}});fireEvent.change(screen.getByLabelText('Start date'),{target:{value:'2027-01-01'}});fireEvent.change(screen.getByLabelText('End date'),{target:{value:'2027-01-01'}});fireEvent.change(screen.getByLabelText('Total budget (LKR)'),{target:{value:'10000'}});fireEvent.click(screen.getByRole('button',{name:'Choose places & meals →'}));}
beforeEach(()=>{
 vi.mocked(useAuth).mockReturnValue({user:{id:7,firstName:'Test',lastName:'User',email:'test@example.com',profileImageUrl:null,role:'USER'},authenticate:vi.fn(),logout:vi.fn(),updateUser:vi.fn()});
 vi.mocked(getAutomaticMealOptions).mockResolvedValue({meals:[]});
 vi.mocked(getDestinations).mockResolvedValue({content:[],totalElements:0,totalPages:0,number:0,size:100});
 vi.mocked(searchTravel).mockImplementation(async search=>({places:[place(search.type)],latitude:6.86,longitude:81.04,location:'Ella',page:0,hasMore:false}));
 vi.mocked(previewManualPlan).mockImplementation(async body=>({days:body.days.map(day=>({date:day.date,area:day.area,visits:day.stops.map(stop=>({stop,startTime:stop.preferredTime||'08:00:00',endTime:stop.mealType==='LUNCH'?'13:15:00':'09:30:00'}))})),unscheduled:[],warnings:['Check opening hours.'],estimatedTotal:1500,remainingBudget:8500}));
 vi.mocked(saveManualPlan).mockResolvedValue({id:99} as Trip);
});
afterEach(()=>{cleanup();vi.resetAllMocks();});
describe('guided manual planning',()=>{
 it('suggests user-selected visits and meals, collects preferences, previews and saves only at the end',async()=>{
  view();basics();await screen.findByRole('heading',{name:'Nine Arch Bridge'});expect(saveManualPlan).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button',{name:'+ Select'}));
  fireEvent.change(screen.getByLabelText('Suggestion category'),{target:{value:'RESTAURANT'}});await screen.findByRole('heading',{name:'Lunch cafe'});fireEvent.click(screen.getByRole('button',{name:'+ Select'}));
  fireEvent.click(screen.getByRole('button',{name:/Set visit preferences/}));
  const durations=screen.getAllByLabelText('Visit duration (minutes)');fireEvent.change(durations[0],{target:{value:'90'}});fireEvent.change(durations[1],{target:{value:'45'}});
  const costs=screen.getAllByLabelText('Group cost (LKR)');expect((costs[0] as HTMLInputElement).value).toBe('0');fireEvent.change(costs[1],{target:{value:'1500'}});
  fireEvent.change(screen.getAllByLabelText('Notes')[0],{target:{value:'Bring camera'}});
  fireEvent.click(screen.getByRole('button',{name:'Build my schedule →'}));await screen.findByRole('heading',{name:'Review your schedule'});
  expect(previewManualPlan).toHaveBeenCalledWith(expect.objectContaining({startingLocation:'Colombo hotel',transferMinutes:5,days:[expect.objectContaining({area:'Ella',stops:[expect.objectContaining({title:'Nine Arch Bridge',durationMinutes:90,notes:'Bring camera'}),expect.objectContaining({title:'Lunch cafe',mealType:'LUNCH',durationMinutes:45,preferredTime:'12:30',estimatedCost:1500})]})]}));
  expect(saveManualPlan).not.toHaveBeenCalled();fireEvent.click(screen.getByRole('button',{name:'Save this schedule →'}));expect(await screen.findByText('Saved itinerary')).toBeTruthy();expect(saveManualPlan).toHaveBeenCalledTimes(1);
 });
 it('retains selections and blocks saving when selected stops do not fit',async()=>{
  vi.mocked(previewManualPlan).mockResolvedValue({days:[],unscheduled:[{date:'2027-01-01',title:'Nine Arch Bridge',reason:'Not enough time.'}],warnings:[],estimatedTotal:0,remainingBudget:10000});
  view();basics();await screen.findByRole('heading',{name:'Nine Arch Bridge'});fireEvent.click(screen.getByRole('button',{name:'+ Select'}));fireEvent.click(screen.getByRole('button',{name:/Set visit preferences/}));
  fireEvent.change(screen.getByLabelText('Visit duration (minutes)'),{target:{value:'720'}});fireEvent.click(screen.getByRole('button',{name:'Build my schedule →'}));
  expect(await screen.findByRole('alert')).toBeTruthy();expect((screen.getByRole('button',{name:'Save this schedule →'}) as HTMLButtonElement).disabled).toBe(true);
  fireEvent.click(screen.getByRole('button',{name:'← Adjust preferences'}));expect((screen.getByLabelText('Visit duration (minutes)') as HTMLInputElement).value).toBe('720');expect(saveManualPlan).not.toHaveBeenCalled();
 });
 it('shows provider errors and lets the user explicitly choose another data source',async()=>{
  vi.mocked(searchTravel).mockRejectedValue(new Error('Provider unavailable'));
  vi.mocked(searchGeoapify).mockResolvedValue({places:[{id:'geo1',name:'Temple',address:'Ella',latitude:6.86,longitude:81.04,categories:['tourism.attraction'],website:null,phone:null,openingHours:null,description:null,wheelchair:null}],latitude:6.86,longitude:81.04,location:'Ella',page:0,hasMore:false});
  view();basics();expect(await screen.findByRole('alert')).toBeTruthy();expect(getPlaces).not.toHaveBeenCalled();
  fireEvent.change(screen.getByLabelText('Suggestions from'),{target:{value:'geoapify'}});expect(await screen.findByRole('heading',{name:'Temple'})).toBeTruthy();
 });
 it('allows meal placeholders with user-supplied times and does not save without reviewing',async()=>{
  view();basics();await screen.findByRole('heading',{name:'Nine Arch Bridge'});fireEvent.click(screen.getByRole('button',{name:'+ lunch'}));fireEvent.click(screen.getByRole('button',{name:/Set visit preferences/}));
  fireEvent.change(screen.getByLabelText('Meal start time'),{target:{value:'13:00'}});fireEvent.click(screen.getByRole('button',{name:'Build my schedule →'}));await screen.findByRole('heading',{name:'Review your schedule'});
  expect(previewManualPlan).toHaveBeenCalledWith(expect.objectContaining({days:[expect.objectContaining({stops:[expect.objectContaining({title:'Lunch stop',mealType:'LUNCH',preferredTime:'13:00',placeSource:'CUSTOM'})]})]}));expect(saveManualPlan).not.toHaveBeenCalled();
 });
});

describe('AI planning review',()=>{
 it('asks practical preferences after basics without requiring manual venue selection',async()=>{
  const client=new QueryClient({defaultOptions:{queries:{retry:false}}});
  render(<QueryClientProvider client={client}><MemoryRouter><GuidedTripPlanner aiMode/></MemoryRouter></QueryClientProvider>);
  basics();expect(screen.getByRole('button',{name:'Generate AI schedule →'})).toBeTruthy();
  expect(screen.getByLabelText('Any dietary needs or preferred cuisines?')).toBeTruthy();
  expect(screen.getByLabelText('Any accessibility or walking limits?')).toBeTruthy();
  expect(screen.getByLabelText('Should AI suggest overnight hotels?')).toBeTruthy();
  expect(screen.queryByLabelText('Nearby venue type')).toBeNull();expect(searchTravel).not.toHaveBeenCalled();
 });

 it('generates from basic details, shows intelligent durations and saves the server draft only after review',async()=>{
  const apiModule=await import('../../api/client');const call=vi.spyOn(apiModule,'api');
  call.mockImplementation(async (url,options)=>{
   if(url==='/trips/ai/preview'){
    const request=JSON.parse(String(options?.body));const plan=request.plan;
    expect(request.pace).toBe('RELAXED');expect(request.meals).toEqual(['BREAKFAST','LUNCH','DINNER']);expect(request.includeHotels).toBe(true);expect(request.preferences).toContain('Vegetarian');expect(plan.days[0].stops).toEqual([]);
    plan.days[0].stops=[{key:'garden',title:'Botanical garden',category:'ATTRACTION',placeSource:'GEOAPIFY',placeId:'garden',address:'Gardens',latitude:51,longitude:-0.1,durationMinutes:180,preferredTime:null,estimatedCost:0,mealType:'NONE',notes:'AI duration estimate: allow time to explore extensive gardens.'}];
    // Backend serialization includes null pins and a different property order.
    const returned={...plan,startingLatitude:null,startingLongitude:null,optimizeTravel:true};
    return {draftId:'draft-1',plan:returned,model:'test-model',explanations:['Relaxed visits with time to explore.'],warnings:['Hours require confirmation.'],preview:{days:[{...plan.days[0],visits:[{stop:plan.days[0].stops[0],startTime:'09:00',endTime:'12:00'}]}],unscheduled:[],warnings:[],estimatedTotal:0,remainingBudget:10000}} as never;
   }
   if(url==='/trips/ai/draft-1/save')return {id:99} as never;
   throw new Error(`Unexpected API request: ${url}`);
  });
  const client=new QueryClient({defaultOptions:{queries:{retry:false}}});render(<QueryClientProvider client={client}><MemoryRouter initialEntries={['/trips/ai']}><Routes><Route path="/trips/ai" element={<GuidedTripPlanner aiMode/>}/><Route path="/trips/:id" element={<p>Saved itinerary</p>}/></Routes></MemoryRouter></QueryClientProvider>);
  basics();fireEvent.change(screen.getByLabelText('Any dietary needs or preferred cuisines?'),{target:{value:'Vegetarian'}});fireEvent.change(screen.getByLabelText('Should AI suggest overnight hotels?'),{target:{value:'yes'}});fireEvent.change(screen.getByLabelText('Trip pace'),{target:{value:'RELAXED'}});fireEvent.click(screen.getByRole('button',{name:'Generate AI schedule →'}));
  await screen.findByRole('heading',{name:'Review your schedule'});expect(screen.getByText(/180 minutes/)).toBeTruthy();expect(screen.getByText('Hours require confirmation.')).toBeTruthy();expect(call).toHaveBeenCalledTimes(1);
  fireEvent.click(screen.getByRole('button',{name:'Save this schedule →'}));await screen.findByText('Saved itinerary');expect(call).toHaveBeenLastCalledWith('/trips/ai/draft-1/save',{method:'POST'});expect(saveManualPlan).not.toHaveBeenCalled();call.mockRestore();
 });
});
