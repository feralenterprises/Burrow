# R10C · Owner design-reference comparison, standalone shell only

## Change
- Added an interactive **Compare / Sprites** visual switch on the 225px Boss Chronicle gallery.
- **Compare** shows cropped portraits from the *owner-supplied approved design screenshot*. These are explicitly identified as **design reference, not verified game art**. They are NOT fed into the verified model-art manifest, the source-backed icon catalog, or live RuneLite code.
- **Sprites** restores the previously bundled authentic small OSRS sprites. It remains the default when the app starts, and after toggling back.
- Boss details also show the reference image with the warning that it is not approved source artwork.
- The three approved design crops are included only inside this standalone review build to permit direct visual comparison. No other UI/gameplay/account logic was changed.

## Gates still open
- 0/3 canonical OSRS NPC model renders verified / approved for distribution; original design screenshot contains illustrations and is not adequate game-art provenance.
- Main TPT plugin untouched; shell uses only three boss fixtures.
- Native RuneLite plugin integration and game event tests remain pending.
