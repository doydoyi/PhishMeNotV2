// Small fetch wrapper for the PhishMeNot API.
// - sends the session cookie (credentials: 'include')
// - echoes the XSRF-TOKEN cookie in the X-XSRF-TOKEN header on state-changing requests

export const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';
export const GOOGLE_LOGIN_URL = `${BACKEND_URL}/oauth2/authorization/google`;

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

function readCookie(name) {
  const match = document.cookie.split('; ').find((c) => c.startsWith(`${name}=`));
  return match ? decodeURIComponent(match.substring(name.length + 1)) : null;
}

async function ensureCsrfCookie() {
  if (!readCookie('XSRF-TOKEN')) {
    await fetch('/api/health', { credentials: 'include' });
  }
  return readCookie('XSRF-TOKEN');
}

export async function api(path, { method = 'GET', body } = {}) {
  const headers = { Accept: 'application/json' };
  if (method !== 'GET') {
    const token = await ensureCsrfCookie();
    if (token) headers['X-XSRF-TOKEN'] = token;
  }
  if (body !== undefined) headers['Content-Type'] = 'application/json';

  let response;
  try {
    response = await fetch(path, {
      method,
      headers,
      credentials: 'include',
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'Cannot reach the PhishMeNot server. Please try again later.');
  }

  if (response.status === 204) return null;
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new ApiError(response.status, data?.message || 'Something went wrong. Please try again.');
  }
  return data;
}
