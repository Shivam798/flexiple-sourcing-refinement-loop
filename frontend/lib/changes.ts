import type { Change } from "./types";

/**
 * The model writes change targets in the schema's snake_case ("filters.min_years_experience")
 * while the UI knows fields by their camelCase names. Comparing on a normalised form keeps
 * the highlight working without constraining how the model phrases the path.
 */
const normalise = (target: string) => target.toLowerCase().replace(/[^a-z0-9]/g, "");

export function isMarked(changes: Change[], field: string): boolean {
  const wanted = normalise(field);
  return changes.some((change) => {
    const target = normalise(change.target);
    return target === wanted || target.startsWith(wanted);
  });
}

/** Turns "filters.min_years_experience" into "Minimum experience" for the change log. */
export function humaniseTarget(target: string): string {
  const leaf = target.split(".").slice(1).join(" ") || target;
  const words = leaf
    .replace(/\[(\d+)\]/g, " $1")
    .replace(/[_-]+/g, " ")
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .trim()
    .toLowerCase();
  return words.charAt(0).toUpperCase() + words.slice(1);
}

/**
 * The model renders an absent value in whatever notation the field uses — "[]", "{}", "null",
 * "none". A recruiter reading the change log should see one word for all of them.
 */
export function renderValue(value: string): string {
  const trimmed = value.trim();
  if (!trimmed || ["[]", "{}", "null", "none", "n/a", '""'].includes(trimmed.toLowerCase())) {
    return "nothing";
  }
  // Strip JSON array notation so ["Backend Engineer"] reads as Backend Engineer.
  return trimmed
    .replace(/^\[|\]$/g, "")
    .replace(/"/g, "")
    .trim() || "nothing";
}
