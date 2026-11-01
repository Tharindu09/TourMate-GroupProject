// @vitest-environment jsdom
import {afterEach, describe, expect, it, vi} from 'vitest';
import {cleanup, render, screen, waitFor} from '@testing-library/react';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {GeoListingPhoto} from './GeoListingPhoto';
import {getGeoPhoto} from '../../services/geoapifyService';
import {type GeoPlace} from '../../models/geoapify';
vi.mock('../../services/geoapifyService', async importOriginal => ({...await importOriginal<typeof import('../../services/geoapifyService')>(), getGeoPhoto: vi.fn()}));
afterEach(() => {cleanup(); vi.clearAllMocks(); vi.unstubAllGlobals();});
const place: GeoPlace = {id: 'arbitrary-place-id', name: 'A new venue', address: 'Anywhere', latitude: 8.5, longitude: 81.2, categories: [], website: null, phone: null, openingHours: null, description: null, wheelchair: null};
function renderPhoto() {return render(<QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}><GeoListingPhoto place={place}/></QueryClientProvider>);}
describe('dynamic place photos', () => {
 it('shows the resolved place image and its author/license/source', async () => {
  vi.mocked(getGeoPhoto).mockResolvedValue({url: 'https://upload.wikimedia.org/new-place.jpg', sourceUrl: 'https://commons.wikimedia.org/wiki/File:New_place.jpg', author: 'Photographer', license: 'CC BY-SA 4.0', licenseUrl: 'https://creativecommons.org/licenses/by-sa/4.0/'});
  renderPhoto();
  expect((await screen.findByRole('img')).getAttribute('src')).toBe('https://upload.wikimedia.org/new-place.jpg');
  expect(screen.getByText('Photographer')).toBeTruthy();
  expect(screen.getByRole('link', {name: 'CC BY-SA 4.0'}).getAttribute('href')).toContain('creativecommons.org');
  expect(screen.getByRole('link', {name: 'Original photo & credits ↗'}).getAttribute('href')).toContain('commons.wikimedia.org');
 });
 it('does not fetch offscreen cards until they are near the viewport', async () => {
  let intersect: IntersectionObserverCallback = () => {};
  const disconnect = vi.fn();
  vi.stubGlobal('IntersectionObserver', class {constructor(callback: IntersectionObserverCallback) {intersect = callback;} observe() {} disconnect = disconnect;});
  vi.mocked(getGeoPhoto).mockResolvedValue({url: null, sourceUrl: null, author: null, license: null, licenseUrl: null});
  renderPhoto(); expect(getGeoPhoto).not.toHaveBeenCalled();
  intersect([{isIntersecting: true} as IntersectionObserverEntry], {} as IntersectionObserver);
  await waitFor(() => expect(getGeoPhoto).toHaveBeenCalledTimes(1));
  expect(disconnect).toHaveBeenCalled();
 });
 it('provides a retry for provider failures without displaying unrelated images', async () => {
  vi.mocked(getGeoPhoto).mockRejectedValue(new Error('Provider offline'));
  renderPhoto(); await screen.findByRole('button', {name: 'Retry photo'});
  expect(screen.queryByRole('img')).toBeNull(); expect(screen.queryByText(/Area photo/)).toBeNull();
 });
});
