export function formatDateTime(value) {
  if (!value) return '—';
  // The API returns UTC timestamps without an offset.
  const date = new Date(value.endsWith('Z') ? value : `${value}Z`);
  return date.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}
