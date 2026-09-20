/**
 * Candidates in this dataset are fictional, so a photograph would be a fabrication. Initials
 * on a tinted ground identify a card at a glance without inventing a face. The hue derives
 * from the profile id, so the same person keeps the same mark across rounds.
 */
export function Avatar({ name, id }: { name: string; id: string }) {
  const initials = name
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0])
    .join("")
    .toUpperCase();

  const hue = [...id].reduce((acc, char) => (acc * 31 + char.charCodeAt(0)) % 360, 7);

  return (
    <span
      aria-hidden="true"
      className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full text-[14px] font-semibold tracking-[-0.01em]"
      style={{
        background: `oklch(0.92 0.045 ${hue})`,
        color: `oklch(0.42 0.09 ${hue})`,
      }}
    >
      {initials}
    </span>
  );
}
