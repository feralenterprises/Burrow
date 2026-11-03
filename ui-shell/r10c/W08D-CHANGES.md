# Wave 8D changes

1. New `WikiBossArtFetcher.java` fetches original `File:<Boss>.png` image metadata and exact PNG from OSRS Wiki; allowlisted HTTPS hosts, strict PNG/alpha/dimension checks, max 8 MiB and exact SHA-256. It stages to `artwork-review/wiki-staged-unapproved`, never to approved artwork.
2. The standalone `BossArtReviewer.java` window now has an asynchronous **Fetch OSRS Wiki** button and a separate **Review Wiki candidate** button, besides local PNG loading. Fetches do not block Swing; failures preserve existing OSRS sprites.
3. New root command `06-FETCH-WIKI-BOSS-MODELS-WINDOWS.cmd` attempts batch downloads without installing anything. Existing shell launcher, artwork status checks, all four tabs, Outfitter and Chronicle separation, and account mock states remain unchanged.
4. Added repeatable tests, original UI screenshots and artwork-review preview. Zero full-size boss models approved or included.
5. The source remains a **standalone shell**, not an importable TPT plugin module. This ZIP is not a RuneLite install or production release.
