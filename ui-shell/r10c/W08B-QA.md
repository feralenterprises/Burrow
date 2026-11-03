# Wave 8B standalone QA

- Baseline: W08A source SHA256 `14fb86896c41d0cfa022a759c14aa80ed002f81bc8ddbf5c5da87c43f6bf8358`
- W08B Java source SHA256 `b54c44850f26621c74df858fae575bc6530fdd08a678e5b983450c9ba2b0a705`
- Java build: `javac --release 17`: PASS
- Offline interaction regression: PASS
- New optional image manifest test: PASS (no manifest rejected; owner-approved=false rejected; incorrect SHA rejected; valid deliberately synthetic 160x160 test fixture accepted *in test only* and removed)
- Run-time art audit: all three `FALLBACK (no approved model file)`
- Screenshot outputs: 72 actual Swing renders, 36 native 225x790 and 36 enlarged 675x2370
- Approved full-resolution model assets: ZERO, not claimed
- Rejected bulky mascot: absent
- Main TPT source changed: NO
- Native Windows RuneLite QA: NOT RUN
- Owner visual approval: NOT OBTAINED
- Status: **Wave 8B checkpoint, HOLD**
