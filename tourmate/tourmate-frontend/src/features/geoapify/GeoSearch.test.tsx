// @vitest-environment jsdom
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {cleanup, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import {GeoSearch} from './GeoSearch';
import {getGeoPhoto, searchGeoapify} from '../../services/geoapifyService';
import {type GeoResults} from '../../models/geoapify';
vi.mock('../../services/geoapifyService', async importOriginal => ({...await importOriginal<typeof import('../../services/geoapifyService')>(), searchGeoapify: vi.fn(), getGeoPhoto: vi.fn()}));
afterEach(() => {cleanup(); vi.clearAllMocks();});
beforeEach(() => vi.mocked(getGeoPhoto).mockResolvedValue({url: null, sourceUrl: null, author: null, license: null, licenseUrl: null}));
function SearchView() {return <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}><GeoSearch/></QueryClientProvider>;}
const response: GeoResults = {places: [], page: 0, hasMore: true, latitude: 6.86, longitude: 81.04, location: 'Ella, Sri Lanka'};
describe('Geoapify search', () => {
 it('waits for submission and paginates the submitted location rather than edited input', async () => {
  vi.mocked(searchGeoapify).mockResolvedValueOnce(response).mockResolvedValueOnce({...response, page: 1, hasMore: false});
  render(<MemoryRouter initialEntries={['/search?q=Ella&type=RESTAURANT']}><SearchView/></MemoryRouter>);
  expect(searchGeoapify).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button', {name: 'Search places →'}));
  await screen.findByRole('button', {name: 'Next results →'});
  expect(searchGeoapify).toHaveBeenCalledWith({q: 'Ella', type: 'RESTAURANT', name: '', radius: 10000}, 0);
  fireEvent.change(screen.getByLabelText('Destination or town'), {target: {value: 'Kandy'}});
  fireEvent.click(screen.getByRole('button', {name: 'Next results →'}));
  await waitFor(() => expect(searchGeoapify).toHaveBeenLastCalledWith({q: 'Ella', type: 'RESTAURANT', name: '', radius: 10000, lat: 6.86, lon: 81.04}, 1));
 });
 it('shows setup errors without substituting database samples', async () => {
  vi.mocked(searchGeoapify).mockRejectedValue(new Error('Geoapify is not configured'));
  render(<MemoryRouter><SearchView/></MemoryRouter>);
  fireEvent.change(screen.getByLabelText('Destination or town'), {target: {value: 'London, UK'}});
  fireEvent.click(screen.getByRole('button', {name: 'Search places →'}));
  expect((await screen.findByRole('alert')).textContent).toContain('Geoapify is not configured');
  expect(screen.queryByText('Hill Country Guesthouse (demo)')).toBeNull();
 });
 it('does not substitute a local or area photograph when the place has no media', async () => {
  vi.mocked(searchGeoapify).mockResolvedValue({...response, hasMore: false, places: [{id: 'place_1', name: 'Test Cafe', address: 'Ella', latitude: 6.86, longitude: 81.04, categories: ['catering.cafe'], website: null, phone: null, openingHours: null, description: null, wheelchair: null}]});
  render(<MemoryRouter><SearchView/></MemoryRouter>);
  fireEvent.change(screen.getByLabelText('Destination or town'), {target: {value: 'London, UK'}});
  fireEvent.click(screen.getByRole('button', {name: 'Search places →'}));
  await screen.findByText('Test Cafe');
  expect(screen.getByRole('link', {name: 'Geoapify'})).toBeTruthy();
  expect(screen.getByRole('link', {name: 'OpenStreetMap contributors'})).toBeTruthy();
  await screen.findByText('Photo unavailable');
  expect(screen.queryByRole('img')).toBeNull();
  expect(screen.queryByText(/★/)).toBeNull();
 });
});
