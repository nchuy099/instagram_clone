import { formatDistanceToNowStrict, isValid } from 'date-fns';

const DEFAULT_LABEL = 'just now';

export function formatRelativePostTime(value?: string | Date | null): string {
  if (!value) {
    return DEFAULT_LABEL;
  }

  const date = value instanceof Date ? value : new Date(value);
  if (!isValid(date)) {
    return DEFAULT_LABEL;
  }

  return formatDistanceToNowStrict(date, { addSuffix: true });
}
