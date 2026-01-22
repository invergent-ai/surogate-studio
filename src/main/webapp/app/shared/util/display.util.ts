export enum MessageLevel {
    INFO,
    WARN,
    ERROR
}

export const truncate = (text: string, length: number) => {
    return text.length > length ? text.substring(0, length) + "..." : text;
}

export const roundUpTo = (value: number, decimals: number = 2): number => {
  if (typeof value !== 'number' || !isFinite(value)) return value;
  if (decimals < 0) decimals = 0;
  const factor = Math.pow(10, decimals);
  return Math.ceil((value + Number.EPSILON) * factor) / factor;
};


