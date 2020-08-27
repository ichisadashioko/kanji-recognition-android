package io.github.ichisadashioko.android.kanji.views.icons;

public class SVGPathSeg {
    // Path Segment Types
    public static final int PATHSEG_UNKNOWN = 0;
    public static final int PATHSEG_CLOSEPATH = 1;
    public static final int PATHSEG_MOVETO_ABS = 2;
    public static final int PATHSEG_MOVETO_REL = 3;
    public static final int PATHSEG_LINETO_ABS = 4;
    public static final int PATHSEG_LINETO_REL = 5;
    public static final int PATHSEG_CURVETO_CUBIC_ABS = 6;
    public static final int PATHSEG_CURVETO_CUBIC_REL = 7;
    public static final int PATHSEG_CURVETO_QUADRATIC_ABS = 8;
    public static final int PATHSEG_CURVETO_QUADRATIC_REL = 9;
    public static final int PATHSEG_ARC_ABS = 10;
    public static final int PATHSEG_ARC_REL = 11;
    public static final int PATHSEG_LINETO_HORIZONTAL_ABS = 12;
    public static final int PATHSEG_LINETO_HORIZONTAL_REL = 13;
    public static final int PATHSEG_LINETO_VERTICAL_ABS = 14;
    public static final int PATHSEG_LINETO_VERTICAL_REL = 15;
    public static final int PATHSEG_CURVETO_CUBIC_SMOOTH_ABS = 16;
    public static final int PATHSEG_CURVETO_CUBIC_SMOOTH_REL = 17;
    public static final int PATHSEG_CURVETO_QUADRATIC_SMOOTH_ABS = 18;
    public static final int PATHSEG_CURVETO_QUADRATIC_SMOOTH_REL = 19;

    public final int pathSegType;

    public float x = 0;
    public float y = 0;
    public float x1 = 0;
    public float y1 = 0;
    public float x2 = 0;
    public float y2 = 0;

    public SVGPathSeg(int pathSegType) {
        this.pathSegType = pathSegType;
    }
}
