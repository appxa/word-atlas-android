import test from "node:test";
import assert from "node:assert/strict";
import { formatPersianDate, shortWeekday } from "./date";

test("weekdays are indexed as names, not characters in a spaced string", () => {
  assert.deepEqual(Array.from({ length: 7 }, (_, i) => shortWeekday(new Date(2026, 10, 1 + i))), ["ی", "د", "س", "چ", "پ", "ج", "ش"]);
});

test("dates display in the Persian calendar", () => {
  assert.match(formatPersianDate(new Date(2026, 10, 5, 12)), /آبان/);
});
