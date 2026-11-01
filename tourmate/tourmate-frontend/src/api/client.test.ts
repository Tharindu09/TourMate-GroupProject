// @vitest-environment jsdom
import {afterEach, describe, expect, it, vi} from 'vitest';
import {api, setAccessToken} from './client';
afterEach(() => {vi.unstubAllGlobals(); setAccessToken(null);});
describe('API client', () => {
 it('attaches the active bearer token to protected requests', async () => {
  const request = vi.fn().mockResolvedValue(new Response(JSON.stringify({id: 1}), {status: 200}));
  vi.stubGlobal('fetch', request); setAccessToken('session-token');
  expect(await api('/users/me')).toEqual({id: 1});
  expect(request.mock.calls[0][1].headers.get('Authorization')).toBe('Bearer session-token');
 });
 it('shows server validation errors', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({errors: {email: 'must be valid'}}), {status: 400})));
  await expect(api('/auth/register')).rejects.toThrow('email: must be valid');
 });
 it('handles non-JSON failures without swallowing the error', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('Bad Gateway', {status: 502})));
  await expect(api('/places')).rejects.toThrow('Unable to complete');
 });
});
