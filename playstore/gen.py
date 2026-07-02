#!/usr/bin/env python3
"""Generate PostScript for Play Store dummy graphics; ghostscript renders them."""
import os

HERE = os.path.dirname(os.path.abspath(__file__))

def load(name):
    with open(os.path.join(HERE, name), "rb") as f:
        return f.read()

def image_block(rgb, w, h, x, y, size_w, size_h):
    """PostScript that draws a w*h RGB image into rect (x,y)-(x+size_w,y+size_h)."""
    hexdata = rgb.hex()
    # wrap hex to 120 chars/line
    lines = "\n".join(hexdata[i:i+120] for i in range(0, len(hexdata), 120))
    return f"""gsave
{x} {y} translate {size_w} {size_h} scale
/picstr {w} 3 mul string def
{w} {h} 8 [{w} 0 0 -{h} 0 {h}]
{{ currentfile picstr readhexstring pop }} false 3 colorimage
{lines}
grestore
"""

def header(w, h):
    return f"<< /PageSize [{w} {h}] >> setpagedevice\n0 0 0 setrgbcolor\n0 0 {w} {h} rectfill\n"

def text(x, y, size, s, r, g, b, font="Helvetica-Bold", center_w=None):
    esc = s.replace("(", r"\(").replace(")", r"\)")
    out = f"/{font} findfont {size} scalefont setfont\n{r} {g} {b} setrgbcolor\n"
    if center_w is not None:
        # center around x within width center_w
        out += f"({esc}) stringwidth pop 2 div {x} {center_w} 2 div add exch sub {y} moveto\n"
    else:
        out += f"{x} {y} moveto\n"
    out += f"({esc}) show\n"
    return out

PURPLE = (0.55, 0.48, 0.80)
TEAL = (0.30, 0.62, 0.66)
WHITE = (0.96, 0.96, 0.98)
GRAY = (0.62, 0.62, 0.70)

icon512 = load("icon512.rgb")
icon320 = load("icon320.rgb")
icon200 = load("icon200.rgb")

# 1) App icon 512x512
with open(f"{HERE}/icon_512.ps", "w") as f:
    f.write(header(512, 512))
    f.write(image_block(icon512, 512, 512, 0, 0, 512, 512))
    f.write("showpage\n")

# 2) Feature graphic 1024x500
with open(f"{HERE}/feature_1024x500.ps", "w") as f:
    f.write(header(1024, 500))
    # subtle purple underline accent
    f.write(f"{PURPLE[0]} {PURPLE[1]} {PURPLE[2]} setrgbcolor\n420 150 540 6 rectfill\n")
    f.write(image_block(icon320, 320, 320, 70, 90, 320, 320))
    f.write(text(430, 295, 96, "ShareCare", *WHITE))
    f.write(text(430, 230, 32, "Organize & share your links", *GRAY))
    f.write(text(430, 172, 26, "for X / Twitter", *TEAL))
    f.write("showpage\n")

# 3) Phone screenshots 1080x1920
def screenshot(path, headline, sub, tag):
    with open(path, "w") as f:
        f.write(header(1080, 1920))
        # top band accent
        f.write(f"{PURPLE[0]} {PURPLE[1]} {PURPLE[2]} setrgbcolor\n0 1720 1080 8 rectfill\n")
        # icon centered near top
        f.write(image_block(icon200, 200, 200, 440, 1470, 200, 200))
        f.write(text(0, 1380, 60, "ShareCare", *WHITE, center_w=1080))
        # headline
        f.write(text(0, 1120, 76, headline, *PURPLE, center_w=1080))
        f.write(text(0, 1040, 40, sub, *GRAY, center_w=1080))
        # mock content cards
        y = 820
        for i in range(3):
            f.write(f"0.10 0.10 0.13 setrgbcolor\n80 {y} 920 160 rectfill\n")
            f.write(f"{TEAL[0]} {TEAL[1]} {TEAL[2]} setrgbcolor\n120 {y+60} 60 60 rectfill\n")
            f.write(text(220, y + 95, 34, "Sample item " + str(i + 1), *WHITE))
            f.write(text(220, y + 45, 26, "example.com/link", *GRAY))
            y -= 200
        f.write(text(0, 90, 30, tag, *GRAY, center_w=1080))
        f.write("showpage\n")

screenshot(f"{HERE}/screenshot_1.ps", "Compose posts", "Draft and edit before you share", "Dummy screenshot 1 of 2")
screenshot(f"{HERE}/screenshot_2.ps", "Organize links", "Tag, filter and search everything", "Dummy screenshot 2 of 2")

print("generated PS files")
