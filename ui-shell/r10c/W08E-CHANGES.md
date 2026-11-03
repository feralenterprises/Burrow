# Wave 8E change record

1. Query MediaWiki `prop=pageimages&piprop=original` for the boss article's lead-original image (prevents guessing only `File:<Boss>.png`).
2. If absent/not PNG/unsupported, fall back to old `File:<Boss>.png` imageinfo route. Network denial does not generate invented art or mask a failure.
3. Strictly validate original-host allowlist, PNG signature/transparency/dimensions, exact SHA-256, and boss allowlist. No automatic owner approval, installation, or live plugin writes.
4. Add independent self-tests for lead-image JSON parsing, malicious host rejection, original fallback, and existing UI regressions.
5. Render and package actual 225px screenshots plus enlarged phone views.

Remaining blockers: 0/3 high-resolution NPC models staged/approved; true game model identity, suitable quality, and redistribution rights require review.
