// @vitest-environment jsdom
import {cleanup, fireEvent, render, screen} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {mapLinks, PlaceMap} from './PlaceMap';
afterEach(cleanup);
describe('OpenStreetMap integration', () => {
 it('loads an exact marker only after the map is opened and lets users close it', () => {
  render(<PlaceMap place={{name: 'Fort Frederick', latitude: 8.572, longitude: 81.242}}/>);
  expect(screen.queryByTitle('OpenStreetMap location of Fort Frederick')).toBeNull();
  fireEvent.click(screen.getByRole('button', {name: 'View on map →'}));
  const frame = screen.getByTitle('OpenStreetMap location of Fort Frederick');
  const url = new URL(frame.getAttribute('src')!);
  expect(url.origin).toBe('https://www.openstreetmap.org');
  expect(url.searchParams.get('marker')).toBe('8.572,81.242');
  expect(screen.getByRole('button').getAttribute('aria-expanded')).toBe('true');
  expect(screen.getByRole('link', {name: 'OpenStreetMap contributors'})).toBeTruthy();
  fireEvent.click(screen.getByRole('button', {name: 'Hide map ↑'}));
  expect(screen.queryByTitle('OpenStreetMap location of Fort Frederick')).toBeNull();
 });
 it('searches by name and address when coordinates are absent, without inventing a marker', () => {
  render(<PlaceMap place={{name: 'Cafe & Tea', address: 'Trincomalee', latitude: null, longitude: null}}/>);
  const link = screen.getByRole('link', {name: 'View on map ↗'});
  const url = new URL(link.getAttribute('href')!);
  expect(url.pathname).toBe('/search');
  expect(url.searchParams.get('query')).toBe('Cafe & Tea, Trincomalee');
  expect(screen.queryByRole('button')).toBeNull();
  expect(screen.getByText(/exact coordinates unavailable/)).toBeTruthy();
 });
 it('resolves missing coordinates on click and reuses them when reopened', async () => {
  const resolve = vi.fn().mockResolvedValue({latitude: 8.574719, longitude: 81.23472});
  render(<PlaceMap place={{name: 'Sri Pathrakali Amman Temple'}} resolveLocation={resolve}/>);
  expect(resolve).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button', {name: 'View on map →'}));
  const frame = await screen.findByTitle('OpenStreetMap location of Sri Pathrakali Amman Temple');
  expect(new URL(frame.getAttribute('src')!).searchParams.get('marker')).toBe('8.574719,81.23472');
  fireEvent.click(screen.getByRole('button', {name: 'Hide map ↑'}));
  fireEvent.click(screen.getByRole('button', {name: 'View on map →'}));
  expect(resolve).toHaveBeenCalledTimes(1);
 });
 it('keeps failed coordinate lookups in the app with a clearly labelled search fallback', async () => {
  render(<PlaceMap place={{name: 'Unknown temple'}} resolveLocation={() => Promise.reject(new Error('Coordinates unavailable'))}/>);
  fireEvent.click(screen.getByRole('button', {name: 'View on map →'}));
  expect(await screen.findByRole('alert')).toBeTruthy();
  expect(screen.getByRole('link', {name: 'Search OpenStreetMap by name ↗'}).getAttribute('href')).toContain('/search?');
  expect(screen.queryByTitle('OpenStreetMap location of Unknown temple')).toBeNull();
 });
 it('shows the map and marker automatically on detail pages', () => {
  render(<PlaceMap autoShow place={{name: 'Temple', latitude: 8.57, longitude: 81.23}}/>);
  expect(screen.getByTitle('OpenStreetMap location of Temple')).toBeTruthy();
  expect(screen.queryByRole('button')).toBeNull();
 });
 it('automatically resolves a detail location without a button click', async () => {
  const resolve = vi.fn().mockResolvedValue({latitude: 8.574719, longitude: 81.23472});
  render(<PlaceMap autoShow place={{name: 'Temple'}} resolveLocation={resolve}/>);
  expect(await screen.findByTitle('OpenStreetMap location of Temple')).toBeTruthy();
  expect(resolve).toHaveBeenCalledTimes(1);
 });
 it('uses worldwide coordinates and does not add a fixed country to name searches', () => {
  const sydney=mapLinks({name:'Harbour',latitude:-33.8688,longitude:151.2093});
  expect(new URL(sydney.embed!).searchParams.get('marker')).toBe('-33.8688,151.2093');
  const search=mapLinks({name:'Museum',address:'Paris, France'});
  expect(new URL(search.url).searchParams.get('query')).toBe('Museum, Paris, France');
 });
 it('rejects invalid coordinates and accepts numeric zero coordinates', () => {
  for (const latitude of [NaN, Infinity, 91]) expect(mapLinks({name: 'Place', latitude, longitude: 81}).embed).toBeNull();
  expect(mapLinks({name: 'Place', latitude: 8, longitude: -181}).embed).toBeNull();
  expect(mapLinks({name: 'Place', latitude: 0, longitude: 0}).embed).toContain('marker=0%2C0');
 });
});
