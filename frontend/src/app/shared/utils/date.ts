/** Formats a date as `yyyy-MM-dd` in local time, the format the backend expects. */
export function toIsoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

export function parseDate(value: string | null): Date | null {
  return value ? new Date(value) : null;
}
