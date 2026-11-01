// @vitest-environment jsdom
import {afterEach,describe,expect,it,vi} from 'vitest';
import {cleanup,fireEvent,render,screen,waitFor} from '@testing-library/react';
import {AutomaticMealSuggestions} from './AutomaticMealSuggestions';
import {getAutomaticMealOptions} from '../../services/manualPlanService';
import type {ManualPlanInput,PlanMealOptionsResponse} from '../../models/manualPlan';
vi.mock('../../services/manualPlanService',()=>({getAutomaticMealOptions:vi.fn()}));
afterEach(()=>{cleanup();vi.resetAllMocks();});
const types=['BREAKFAST','LUNCH','DINNER'] as const;const times=['08:00','12:30','19:00'];
const plan:ManualPlanInput={trip:{name:'Trip',startDate:'2027-01-01',endDate:'2027-01-01',travelers:1,budget:0,destinationIds:[],status:'DRAFT'},startingLocation:'Home',transportMode:'CAR',transferMinutes:5,days:[{date:'2027-01-01',area:'Area',startTime:'08:00',endTime:'20:00',stops:types.map((meal,i)=>({key:meal,title:meal,category:'RESTAURANT',placeSource:'CUSTOM',placeId:null,address:null,latitude:null,longitude:null,durationMinutes:60,preferredTime:times[i],estimatedCost:0,notes:'',mealType:meal}))}]};
const response:PlanMealOptionsResponse={meals:types.map((meal,i)=>({key:meal,mealType:meal,at:times[i],error:null,suggestions:{anchor:{latitude:7+i,longitude:80,label:`Position ${i}`},at:times[i],context:`Estimated location for ${meal.toLowerCase()}`,mode:'CAR',warnings:[],options:[{place:{id:meal,name:`${meal.toLowerCase()} cafe`,address:'Along the route',latitude:7+i,longitude:80,categories:['catering.restaurant'],website:null,phone:null,openingHours:null,description:null,wheelchair:null},travelMinutes:3+i,distanceMeters:1000}]}}))};
describe('automatic meal suggestions',()=>{
 it('loads all three added meals automatically and waits for an explicit venue choice',async()=>{
  vi.mocked(getAutomaticMealOptions).mockResolvedValue(response);const select=vi.fn();render(<AutomaticMealSuggestions plan={plan} date="2027-01-01" onSelect={select}/>);
  expect(await screen.findByRole('heading',{name:'breakfast cafe'},{timeout:3000})).toBeTruthy();expect(screen.getByRole('heading',{name:'lunch cafe'})).toBeTruthy();expect(screen.getByRole('heading',{name:'dinner cafe'})).toBeTruthy();
  expect(getAutomaticMealOptions).toHaveBeenCalledTimes(1);expect(select).not.toHaveBeenCalled();fireEvent.click(screen.getByRole('button',{name:'Choose for lunch'}));expect(select).toHaveBeenCalledWith(response.meals[1].suggestions!.options[0].place,'LUNCH','12:30');
 });
 it('refreshes venue suggestions when the chosen meal time changes',async()=>{
  vi.mocked(getAutomaticMealOptions).mockResolvedValue(response);const view=render(<AutomaticMealSuggestions plan={plan} date="2027-01-01" onSelect={vi.fn()}/>);await screen.findByRole('heading',{name:'lunch cafe'},{timeout:3000});
  const changed=structuredClone(plan);changed.days[0].stops[1].preferredTime='13:30';view.rerender(<AutomaticMealSuggestions plan={changed} date="2027-01-01" onSelect={vi.fn()}/>);
  await waitFor(()=>expect(getAutomaticMealOptions).toHaveBeenCalledTimes(2),{timeout:3000});expect(vi.mocked(getAutomaticMealOptions).mock.calls[1][0].days[0].stops[1].preferredTime).toBe('13:30');
 });
 it('does not request automatic options for a meal with a selected map location',()=>{
  const selected=structuredClone(plan);selected.days[0].stops=selected.days[0].stops.map(s=>({...s,latitude:7,longitude:80}));render(<AutomaticMealSuggestions plan={selected} date="2027-01-01" onSelect={vi.fn()}/>);expect(getAutomaticMealOptions).not.toHaveBeenCalled();expect(screen.queryByRole('heading')).toBeNull();
 });
});
