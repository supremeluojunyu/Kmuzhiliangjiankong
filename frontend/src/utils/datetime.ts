import dayjs, { type Dayjs } from 'dayjs';
import utc from 'dayjs/plugin/utc';
import timezone from 'dayjs/plugin/timezone';

dayjs.extend(utc);
dayjs.extend(timezone);

/** 业务时区：北京时间 */
export const APP_TIMEZONE = 'Asia/Shanghai';

export const DATETIME_FMT = 'YYYY-MM-DD HH:mm:ss';

export function parseDateTime(value?: string | null): Dayjs | null {
  if (!value) return null;
  const normalized = value.trim().replace('T', ' ').slice(0, 19);
  const d = dayjs.tz(normalized, DATETIME_FMT, APP_TIMEZONE);
  return d.isValid() ? d : null;
}

export function formatDateTime(value?: Dayjs | string | null): string | undefined {
  if (!value) return undefined;
  const d = dayjs.isDayjs(value) ? value : parseDateTime(String(value));
  return d ? d.format(DATETIME_FMT) : undefined;
}

export function displayDateTime(value?: string | null): string {
  return formatDateTime(value) ?? '—';
}
