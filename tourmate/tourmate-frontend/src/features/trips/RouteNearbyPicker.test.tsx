// @vitest-environment jsdom
import {afterEach,describe,expect,it,vi} from 'vitest';
import {cleanup,fireEvent,render,screen,waitFor} from '@testing-library/react';
import {RouteNearbyPicker} from './RouteNearbyPicker';
import {getRouteNearby,getAiRouteNearby} from '../../services/manualPlanService';
import type {ManualPlanInput,PlanNearbyResponse} from '../../models/manualPlan';
vi.mock('../../services/manualPlanService',()=>({getRouteNearby:vi.fn(),getAiRouteNearby:vi.fn()}));
afterEach(()=>{cleanup();vi.resetAllMocks();});
const plan:ManualPlanInput={trip:{name:'Trip',startDate:'2027-01-01',endDate:'2027-01-01',budget:10000,travelers:2,destinationIds:[],status:'DRAFT'},startingLocation:'Home',transportMode:'WALKING',transferMinutes:5,days:[{date:'2027-01-01',area:'Next town',startTime:'08:00',endTime:'20:00',stops:[]}]};
const result:PlanNearbyResponse={anchor:{latitude:7.3,longitude:80.3,label:'On route'},at:'12:30',context:'Estimated position while traveling from Home to Next town',mode:'WALKING',options:[{place:{id:'cafe',name:'Route cafe',address:'Near the mapped route',latitude:7.31,longitude:80.31,categories:['catering.restaurant'],website:null,phone:null,openingHours:null,description:null,wheelchair:null},travelMinutes:6,distanceMeters:450}],warnings:['Rebuild to verify the detour.']};
describe('route-based meal and hotel options',()=>{
 it('requests AI ranking for the chosen time and preferences, then waits for the user to select',async()=>{
  vi.mocked(getAiRouteNearby).mockResolvedValue({nearby:result,reasons:{cafe:'A short detour; dietary suitability needs confirmation.'},model:'test'});
  const select=vi.fn();render(<RouteNearbyPicker plan={plan} date="2027-01-01" onSelect={select}/>);
  fireEvent.click(screen.getByLabelText('Rank nearby options with AI'));
  fireEvent.change(screen.getByLabelText('Meal or hotel preferences'),{target:{value:'Vegetarian lunch'}});
  fireEvent.click(screen.getByRole('button',{name:'Find AI-ranked nearby options'}));
  expect(await screen.findByText('A short detour; dietary suitability needs confirmation.')).toBeTruthy();
  expect(getAiRouteNearby).toHaveBeenCalledWith(plan,'2027-01-01','12:30','RESTAURANT','Vegetarian lunch');
  expect(getRouteNearby).not.toHaveBeenCalled();expect(select).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button',{name:'Select lunch venue'}));expect(select).toHaveBeenCalledWith(result.options[0].place,'LUNCH','12:30');
 });
 it('reports unavailable AI without silently switching providers',async()=>{
  vi.mocked(getAiRouteNearby).mockRejectedValue(new Error('Ollama is unavailable'));
  render(<RouteNearbyPicker plan={plan} date="2027-01-01" onSelect={vi.fn()}/>);
  fireEvent.click(screen.getByLabelText('Rank nearby options with AI'));fireEvent.click(screen.getByRole('button',{name:'Find AI-ranked nearby options'}));
  expect(await screen.findByRole('alert')).toBeTruthy();expect(getRouteNearby).not.toHaveBeenCalled();
 });

 it('queries the selected time and mode and adds only the venue the user chooses',async()=>{
  vi.mocked(getRouteNearby).mockResolvedValue(result);const select=vi.fn();render(<RouteNearbyPicker plan={plan} date="2027-01-01" onSelect={select}/>);
  expect(getRouteNearby).not.toHaveBeenCalled();fireEvent.click(screen.getByRole('button',{name:'Find nearby route options'}));
  expect(await screen.findByRole('heading',{name:'Route cafe'})).toBeTruthy();expect(screen.getByText(/Estimated position while traveling/)).toBeTruthy();expect(screen.getByText(/6 min/)).toBeTruthy();expect(select).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button',{name:'Select lunch venue'}));expect(select).toHaveBeenCalledWith(result.options[0].place,'LUNCH','12:30');expect(getRouteNearby).toHaveBeenCalledWith(plan,'2027-01-01','12:30','RESTAURANT');
 });
 it('supports hotel choices near the day endpoint without calling them confirmed bookings',async()=>{
  vi.mocked(getRouteNearby).mockResolvedValue({...result,options:[{...result.options[0],place:{...result.options[0].place,name:'Route hotel',categories:['accommodation.hotel']}}]});
  const select=vi.fn();render(<RouteNearbyPicker plan={plan} date="2027-01-01" onSelect={select}/>);fireEvent.change(screen.getByLabelText('Nearby venue type'),{target:{value:'HOTEL'}});
  expect((screen.getByLabelText('Find options near me at') as HTMLInputElement).value).toBe('20:00');fireEvent.click(screen.getByRole('button',{name:'Find nearby route options'}));await screen.findByRole('heading',{name:'Route hotel'});fireEvent.click(screen.getByRole('button',{name:'Select hotel'}));expect(select).toHaveBeenCalledWith(expect.objectContaining({name:'Route hotel'}),'NONE','20:00');
 });
 it('shows routing failures instead of substituting venues from the destination town',async()=>{
  vi.mocked(getRouteNearby).mockRejectedValue(new Error('No public transport route is available'));
  render(<RouteNearbyPicker plan={plan} date="2027-01-01" onSelect={vi.fn()}/>);fireEvent.click(screen.getByRole('button',{name:'Find nearby route options'}));expect(await screen.findByRole('alert')).toBeTruthy();expect(screen.queryByRole('button',{name:/Select .*venue/})).toBeNull();
 });
});
