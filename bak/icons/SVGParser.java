package io.github.ichisadashioko.android.kanji.views.icons;

import java.util.ArrayList;

public class SVGParser {
    public static int SkipSeparators(char[] d, int curPos) {
        while (curPos < d.length) {
            char c = d[curPos];

            if ((c == ' ') || (c == ',') || (c == '\n') || (c == '\t') || (c == '\r')) {
                curPos++;
                continue;
            } else {
                break;
            }
        }

        return curPos;
    }

    public static int SkipIntegerChars(char[] d, int curPos) {
        while (curPos < d.length) {
            char c = d[curPos];
            // [0-9] ASCII range
            if ((c > 47) && (c < 58)) {
                curPos++;
                continue;
            } else {
                break;
            }
        }

        return curPos;
    }

    public static ParseNumberRetval ParseNumber(char[] d, int curPos) throws ParseException {
        curPos = SkipSeparators(d, curPos);

        int startPos = -1;

        if (d[curPos] == '-') {
            // number string starts with negative sign
            startPos = curPos;
            curPos++;
            // parse integer part
            curPos = SkipIntegerChars(d, curPos);
            if (curPos == startPos) {
                // TODO
                throw new ParseException();
            }

            if (curPos < d.length) {
                if (d[curPos] == '.') {
                    curPos++;
                    int prevPos = curPos;
                    curPos = SkipIntegerChars(d, curPos);
                    if (curPos == prevPos) {
                        // TODO
                        throw new ParseException();
                    }
                }
            }
        } else if (d[curPos] == '.') {
            // number string starts with a dot
            curPos++;
            if (curPos < d.length) {
                int prevPos = curPos;
                curPos = SkipIntegerChars(d, curPos);
                if (curPos == prevPos) {
                    // TODO
                    throw new ParseException();
                }
            } else {
                // TODO
                throw new ParseException();
            }
        }

        if (startPos == -1) {
            // TODO
            throw new ParseException();
        } else if (curPos < startPos) {
            // TODO
            throw new ParseException();
        }

        StringBuilder sb = new StringBuilder();

        for (int i = startPos; i < curPos; i++) {
            sb.append(d[i]);
        }

        String s = sb.toString();
        float number = Float.parseFloat(s);
        ParseNumberRetval retval = new ParseNumberRetval();
        retval.number = number;
        retval.curPos = curPos;

        return retval;
    }

    public static ParseNumberParamsRetval ParseNumberParams(int numParams, char[] d, int curPos) throws ParseException {
        if (numParams < 1) {
            // TODO
            throw new ParseException();
        }

        ParseNumberRetval parseNumberRetval;
        float[] params = new float[numParams];

        for (int i = 0; i < numParams; i++) {
            parseNumberRetval = ParseNumber(d, curPos);
            params[i] = parseNumberRetval.number;
            curPos = parseNumberRetval.curPos;
        }

        ParseNumberParamsRetval retval = new ParseNumberParamsRetval();
        retval.curPos = curPos;
        retval.params = params;

        return retval;
    }

    public static ParsePathSegRetval ParseMoveToAbsParams(char[] d, int curPos) throws ParseException {
        ParseNumberParamsRetval parseNumberParamsRetval = ParseNumberParams(2, d, curPos);

        SVGPathSeg seg = new SVGPathSeg(SVGPathSeg.PATHSEG_MOVETO_ABS);

        seg.x = parseNumberParamsRetval.params[0];
        seg.y = parseNumberParamsRetval.params[1];

        curPos = parseNumberParamsRetval.curPos;

        ParsePathSegRetval retval = new ParsePathSegRetval();
        retval.segment = seg;
        retval.curPos = curPos;
        return retval;
    }

    public static ParsePathSegRetval ParseMoveToRelParams(char[] d, int curPos) throws ParseException {
        ParseNumberParamsRetval parseNumberParamsRetval = ParseNumberParams(2, d, curPos);

        SVGPathSeg seg = new SVGPathSeg(SVGPathSeg.PATHSEG_MOVETO_REL);

        seg.x = parseNumberParamsRetval.params[0];
        seg.y = parseNumberParamsRetval.params[1];

        curPos = parseNumberParamsRetval.curPos;

        ParsePathSegRetval retval = new ParsePathSegRetval();
        retval.segment = seg;
        retval.curPos = curPos;
        return retval;
    }


    public static ParsePathSegRetval ParseLineToAbsParams(char[] d, int curPos) throws ParseException {
        ParseNumberParamsRetval parseNumberParamsRetval = ParseNumberParams(2, d, curPos);

        SVGPathSeg seg = new SVGPathSeg(SVGPathSeg.PATHSEG_LINETO_ABS);

        seg.x = parseNumberParamsRetval.params[0];
        seg.y = parseNumberParamsRetval.params[1];

        curPos = parseNumberParamsRetval.curPos;

        ParsePathSegRetval retval = new ParsePathSegRetval();
        retval.segment = seg;
        retval.curPos = curPos;
        return retval;
    }

    public static ParsePathSegRetval ParseLineToRelParams(char[] d, int curPos) throws ParseException {
        ParseNumberParamsRetval parseNumberParamsRetval = ParseNumberParams(2, d, curPos);

        SVGPathSeg seg = new SVGPathSeg(SVGPathSeg.PATHSEG_LINETO_REL);

        seg.x = parseNumberParamsRetval.params[0];
        seg.y = parseNumberParamsRetval.params[1];

        curPos = parseNumberParamsRetval.curPos;

        ParsePathSegRetval retval = new ParsePathSegRetval();
        retval.segment = seg;
        retval.curPos = curPos;
        return retval;
    }

    public static ParsePathSegRetval ParseLineToHorizontalAbsParams(char[] d, int curPos) throws ParseException {
        SVGPathSeg seg = new SVGPathSeg(SVGPathSeg.PATHSEG_LINETO_HORIZONTAL_ABS);
        ParseNumberRetval parseNumberRetval = ParseNumber(d, curPos);
        seg.x = parseNumberRetval.number;
        curPos = parseNumberRetval.curPos;

        ParsePathSegRetval retval = new ParsePathSegRetval();
        retval.curPos = curPos;
        retval.segment = seg;
        return retval;
    }

    public static ParsePathSegRetval ParseLineToHorizontalRelParams(char[] d, int curPos) throws ParseException {
        SVGPathSeg seg = new SVGPathSeg(SVGPathSeg.PATHSEG_LINETO_HORIZONTAL_REL);
        ParseNumberRetval parseNumberRetval = ParseNumber(d, curPos);
        seg.x = parseNumberRetval.number;
        curPos = parseNumberRetval.curPos;

        ParsePathSegRetval retval = new ParsePathSegRetval();
        retval.curPos = curPos;
        retval.segment = seg;
        return retval;
    }

    public static ParsePathSegRetval ParseLineToVerticalAbsParams(char[] d, int curPos) throws ParseException {
        SVGPathSeg seg = new SVGPathSeg(SVGPathSeg.PATHSEG_LINETO_VERTICAL_ABS);
        ParseNumberRetval parseNumberRetval = ParseNumber(d, curPos);
        seg.y = parseNumberRetval.number;
        curPos = parseNumberRetval.curPos;

        ParsePathSegRetval retval = new ParsePathSegRetval();
        retval.curPos = curPos;
        retval.segment = seg;
        return retval;
    }

    public static ParsePathSegRetval ParseLineToVerticalRelParams(char[] d, int curPos) throws ParseException {
        SVGPathSeg seg = new SVGPathSeg(SVGPathSeg.PATHSEG_LINETO_VERTICAL_REL);
        ParseNumberRetval parseNumberRetval = ParseNumber(d, curPos);
        seg.y = parseNumberRetval.number;
        curPos = parseNumberRetval.curPos;

        ParsePathSegRetval retval = new ParsePathSegRetval();
        retval.curPos = curPos;
        retval.segment = seg;
        return retval;
    }

    public static ParsePathSegRetval ParseCurveToCubicAbsParams(char[] d, int curPos) throws ParseException {
        ParseNumberParamsRetval parseNumberParamsRetval = ParseNumberParams(3, d, curPos);

        SVGPathSeg seg = new SVGPathSeg(SVGPathSeg.PATHSEG_CURVETO_CUBIC_ABS);

        seg.x1 = parseNumberParamsRetval.params[0];
        seg.y1 = parseNumberParamsRetval.params[1];
        seg.x2 = parseNumberParamsRetval.params[2];
        seg.y2 = parseNumberParamsRetval.params[3];
        seg.x = parseNumberParamsRetval.params[4];
        seg.y = parseNumberParamsRetval.params[5];

        curPos = parseNumberParamsRetval.curPos;

        ParsePathSegRetval retval = new ParsePathSegRetval();
        retval.segment = seg;
        retval.curPos = curPos;
        return retval;
    }

    public static ParsePathSegRetval ParseCurveToCubicRelParams(char[] d, int curPos) throws ParseException {
        ParseNumberParamsRetval parseNumberParamsRetval = ParseNumberParams(3, d, curPos);

        SVGPathSeg seg = new SVGPathSeg(SVGPathSeg.PATHSEG_CURVETO_CUBIC_REL);

        seg.x1 = parseNumberParamsRetval.params[0];
        seg.y1 = parseNumberParamsRetval.params[1];
        seg.x2 = parseNumberParamsRetval.params[2];
        seg.y2 = parseNumberParamsRetval.params[3];
        seg.x = parseNumberParamsRetval.params[4];
        seg.y = parseNumberParamsRetval.params[5];

        curPos = parseNumberParamsRetval.curPos;

        ParsePathSegRetval retval = new ParsePathSegRetval();
        retval.segment = seg;
        retval.curPos = curPos;
        return retval;
    }

    public static ParsePathSegRetval ParseCurveToCubicSmoothAbsParams(char[] d, int curPos) throws ParseException {
        ParseNumberParamsRetval parseNumberParamsRetval = ParseNumberParams(2, d, curPos);

        SVGPathSeg seg = new SVGPathSeg(SVGPathSeg.PATHSEG_CURVETO_CUBIC_SMOOTH_ABS);

        seg.x2 = parseNumberParamsRetval.params[0];
        seg.y2 = parseNumberParamsRetval.params[1];
        seg.x = parseNumberParamsRetval.params[2];
        seg.y = parseNumberParamsRetval.params[3];

        curPos = parseNumberParamsRetval.curPos;

        ParsePathSegRetval retval = new ParsePathSegRetval();
        retval.segment = seg;
        retval.curPos = curPos;
        return retval;
    }

    public static ParsePathSegRetval ParseCurveToCubicSmoothRelParams(char[] d, int curPos) throws ParseException {
        ParseNumberParamsRetval parseNumberParamsRetval = ParseNumberParams(2, d, curPos);

        SVGPathSeg seg = new SVGPathSeg(SVGPathSeg.PATHSEG_CURVETO_CUBIC_SMOOTH_REL);

        seg.x2 = parseNumberParamsRetval.params[0];
        seg.y2 = parseNumberParamsRetval.params[1];
        seg.x = parseNumberParamsRetval.params[2];
        seg.y = parseNumberParamsRetval.params[3];

        curPos = parseNumberParamsRetval.curPos;

        ParsePathSegRetval retval = new ParsePathSegRetval();
        retval.segment = seg;
        retval.curPos = curPos;
        return retval;
    }

    public static boolean IsValidNumberStartingChar(char c) {
        if (c == '.') {
            return true;
        } else if (c == '-') {
            return true;
        } else if ((c >= '0') && (c <= '9')) {
            return true;
        } else {
            return false;
        }
    }

    public static SVGPathSeg[] ParseSVGPathData(char[] d) throws ParseException {
        int curPos = 0;
        ParsePathSegRetval pathSegRetval;
        SVGPathSeg lastSeg = null;

        ArrayList<SVGPathSeg> pathSegs = new ArrayList<>();

        while (curPos < d.length) {
            char c = d[curPos];
            curPos++;

            if (d[curPos] == 'M') {
                // absolute `moveto` command
                pathSegRetval = ParseMoveToAbsParams(d, curPos + 1);
            } else if (d[curPos] == 'm') {
                // relative `moveto` command
                pathSegRetval = ParseMoveToRelParams(d, curPos + 1);
            } else if (d[curPos] == 'L') {
                // absolute `lineto` command
                pathSegRetval = ParseLineToAbsParams(d, curPos + 1);
            } else if (d[curPos] == 'l') {
                // relative `lineto` command
                pathSegRetval = ParseLineToRelParams(d, curPos + 1);
            } else if (d[curPos] == 'C') {
                // absolute `curveto` command
                pathSegRetval = ParseCurveToCubicAbsParams(d, curPos + 1);
            } else if (d[curPos] == 'c') {
                // relative `curveto` command
                pathSegRetval = ParseCurveToCubicRelParams(d, curPos + 1);
            } else if (d[curPos] == 'S') {
                // absolute `smoothcurveto` command
                pathSegRetval = ParseCurveToCubicSmoothAbsParams(d, curPos + 1);
            } else if (d[curPos] == 's') {
                // relative `smoothcurveto` command
                pathSegRetval = ParseCurveToCubicSmoothRelParams(d, curPos + 1);
            } else if ((d[curPos] == 'z') || (d[curPos] == 'Z')) {
                pathSegRetval = new ParsePathSegRetval();
                pathSegRetval.curPos = curPos + 1;
                pathSegRetval.segment = new SVGPathSeg(SVGPathSeg.PATHSEG_CLOSEPATH);
            } else if (IsValidNumberStartingChar(d[curPos])) {
                if (lastSeg == null) {
                    // TODO
                    throw new ParseException();
                } else if (lastSeg.pathSegType == SVGPathSeg.PATHSEG_MOVETO_ABS) {
                    pathSegRetval = ParseMoveToAbsParams(d, curPos);
                } else if (lastSeg.pathSegType == SVGPathSeg.PATHSEG_MOVETO_REL) {
                    pathSegRetval = ParseMoveToRelParams(d, curPos);
                } else if (lastSeg.pathSegType == SVGPathSeg.PATHSEG_LINETO_ABS) {
                    pathSegRetval = ParseLineToAbsParams(d, curPos);
                } else if (lastSeg.pathSegType == SVGPathSeg.PATHSEG_LINETO_REL) {
                    pathSegRetval = ParseLineToRelParams(d, curPos);
                } else if (lastSeg.pathSegType == SVGPathSeg.PATHSEG_LINETO_HORIZONTAL_ABS) {
                    pathSegRetval = ParseLineToHorizontalAbsParams(d, curPos);
                } else if (lastSeg.pathSegType == SVGPathSeg.PATHSEG_LINETO_HORIZONTAL_REL) {
                    pathSegRetval = ParseLineToHorizontalRelParams(d, curPos);
                } else if (lastSeg.pathSegType == SVGPathSeg.PATHSEG_LINETO_VERTICAL_ABS) {
                    pathSegRetval = ParseLineToVerticalAbsParams(d, curPos);
                } else if (lastSeg.pathSegType == SVGPathSeg.PATHSEG_LINETO_VERTICAL_REL) {
                    pathSegRetval = ParseLineToVerticalRelParams(d, curPos);
                } else if (lastSeg.pathSegType == SVGPathSeg.PATHSEG_CURVETO_CUBIC_ABS) {
                    pathSegRetval = ParseCurveToCubicAbsParams(d, curPos);
                } else if (lastSeg.pathSegType == SVGPathSeg.PATHSEG_CURVETO_CUBIC_REL) {
                    pathSegRetval = ParseCurveToCubicRelParams(d, curPos);
                } else if (lastSeg.pathSegType == SVGPathSeg.PATHSEG_CURVETO_CUBIC_SMOOTH_ABS) {
                    pathSegRetval = ParseCurveToCubicSmoothAbsParams(d, curPos);
                } else if (lastSeg.pathSegType == SVGPathSeg.PATHSEG_CURVETO_CUBIC_SMOOTH_REL) {
                    pathSegRetval = ParseCurveToCubicSmoothRelParams(d, curPos);
                } else {
                    // TODO
                    throw new ParseException();
                }
            } else {
                // TODO
                throw new ParseException();
            }

            curPos = pathSegRetval.curPos;
            lastSeg = pathSegRetval.segment;
            pathSegs.add(lastSeg);
        }

        return (SVGPathSeg[]) pathSegs.toArray();
    }
}
