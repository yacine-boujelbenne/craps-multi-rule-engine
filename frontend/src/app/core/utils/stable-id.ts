/** Deterministic UUID-shaped id for public mock reference data. */
export function stableUuid(seed: string): string {
  const part = (index: number) => {
    let hash = 2166136261;
    for (const char of `${seed}#${index}`) {
      hash ^= char.charCodeAt(0);
      hash = Math.imul(hash, 16777619);
    }
    return (hash >>> 0).toString(16).padStart(8, '0');
  };
  return `${part(1)}-${part(2).slice(0, 4)}-4${part(2).slice(5, 8)}-8${part(3).slice(1, 4)}-${part(3).slice(4)}${part(4)}`;
}
