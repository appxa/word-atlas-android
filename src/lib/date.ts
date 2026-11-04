/** Display dates in the Persian calendar without changing storage keys. */
export function formatPersianDate(date: Date): string {
  return new Intl.DateTimeFormat("fa-IR-u-ca-persian", { weekday: "long", day: "numeric", month: "long", year: "numeric" }).format(date);
}

export function shortWeekday(date: Date): string {
  return ["ی", "د", "س", "چ", "پ", "ج", "ش"][date.getDay()];
}
