/** Three entries with one marked: a shortlist, which is the only thing this product makes. */
export function Wordmark({ className = "" }: { className?: string }) {
  return (
    <span className={`inline-flex items-center gap-2 ${className}`} translate="no">
      <svg width="17" height="17" viewBox="0 0 24 24" aria-hidden="true" className="shrink-0">
        <rect x="3" y="4.5" width="18" height="3" rx="1.5" className="fill-faint" />
        <rect x="3" y="10.5" width="18" height="3" rx="1.5" className="fill-accent" />
        <rect x="3" y="16.5" width="18" height="3" rx="1.5" className="fill-faint" />
      </svg>
      <span className="text-[15px] font-semibold tracking-[-0.02em]">Shortlist</span>
    </span>
  );
}
