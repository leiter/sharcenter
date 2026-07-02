# Play Store listing assets (dummy)

Placeholder graphics generated from the app launcher icon
(`mipmap-*/ic_launcher_foreground` on the black adaptive-icon background)
for uploading a Google Play **Store listing**. Text/branding: **ShareCare**.

| File | Size | Format | Play Store slot |
|------|------|--------|-----------------|
| `hi_res_icon_512.png` | 512×512 | 32-bit PNG | App icon (Hi-res icon) |
| `feature_graphic_1024x500.png` | 1024×500 | 24-bit PNG | Feature graphic |
| `phone_screenshot_1.png` | 1080×1920 | 24-bit PNG | Phone screenshot (min 2) |
| `phone_screenshot_2.png` | 1080×1920 | 24-bit PNG | Phone screenshot (min 2) |

These satisfy Play Console's minimum required media (hi-res icon, feature
graphic, and ≥2 phone screenshots). The screenshots are clearly marked
"Dummy screenshot" and are placeholders — replace with real captures before
a public release.

## Regenerating

Rendered with **ghostscript** from generated PostScript (ImageMagick only
decodes the WebP icon to raw RGB, since gs can't read WebP):

```bash
# from the app module root
convert app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.webp \
    -background black -flatten -filter Lanczos -resize 512x512 fg_on_black.png
convert fg_on_black.png -resize 512x512 -depth 8 rgb:icon512.rgb
convert fg_on_black.png -resize 320x320 -depth 8 rgb:icon320.rgb
convert fg_on_black.png -resize 200x200 -depth 8 rgb:icon200.rgb
python3 gen.py            # emits *.ps next to the .rgb files
gs -dQUIET -dBATCH -dNOPAUSE -sDEVICE=pngalpha -g512x512 \
   -dGraphicsAlphaBits=4 -dTextAlphaBits=4 -o hi_res_icon_512.png icon_512.ps
gs -dQUIET -dBATCH -dNOPAUSE -sDEVICE=png16m  -g1024x500 \
   -dGraphicsAlphaBits=4 -dTextAlphaBits=4 -o feature_graphic_1024x500.png feature_1024x500.ps
gs -dQUIET -dBATCH -dNOPAUSE -sDEVICE=png16m  -g1080x1920 \
   -dGraphicsAlphaBits=4 -dTextAlphaBits=4 -o phone_screenshot_1.png screenshot_1.ps
gs -dQUIET -dBATCH -dNOPAUSE -sDEVICE=png16m  -g1080x1920 \
   -dGraphicsAlphaBits=4 -dTextAlphaBits=4 -o phone_screenshot_2.png screenshot_2.ps
```

`gen.py` reads the `icon*.rgb` raw pixel dumps and emits PostScript that
inlines the icon (`colorimage`) and draws the text with ghostscript's
built-in Helvetica-Bold.
