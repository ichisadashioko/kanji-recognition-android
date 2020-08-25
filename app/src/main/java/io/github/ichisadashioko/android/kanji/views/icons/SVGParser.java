package io.github.ichisadashioko.android.kanji.views.icons;

import java.util.ArrayList;
import java.util.List;

class SVGParser
{
    public static int SkipSeparators(char[] d, int curPos)
    {
        while (curPos < d.length)
        {
            char c = d[curPos];

            if ((c == ' ') || (c == ',') || (c == '\n') || (c == '\t') || (c == '\r'))
            {
                curPos++;
                continue;
            }
            else
            {
                break;
            }
        }

        return curPos;
    }

    public static int SkipIntegerChars(char[] d, int curPos)
    {
        while (curPos < d.length)
        {
            char c = d[curPos];
            // [0-9] ASCII range
            if ((c > 47) && (c < 58))
            {
                curPos++;
                continue;
            }
            else
            {
                break;
            }
        }

        return curPos;
    }

    public static ParseNumberRetval ParseNumber(char[] d, int curPos) throws ParseException
    {
        curPos = SkipSeparators(d, curPos);

        int startPos = -1;

        if (d[curPos] == '-')
        {
            // number string starts with negative sign
            startPos = curPos;
            curPos++;
            // parse integer part
            curPos = SkipIntegerChars(d, curPos);
            if (curPos == startPos)
            {
                // TODO
                throw new ParseException();
            }

            if (curPos < d.length)
            {
                if (d[curPos] == '.')
                {
                    curPos++;
                    int prevPos = curPos;
                    curPos      = SkipIntegerChars(d, curPos);
                    if (curPos == prevPos)
                    {
                        // TODO
                        throw new ParseException();
                    }
                }
            }
        }
        else if (d[curPos] == '.')
        {
            // number string starts with a dot
            curPos++;
            if (curPos < d.length)
            {
                int prevPos = curPos;
                curPos      = SkipIntegerChars(d, curPos);
                if (curPos == prevPos)
                {
                    // TODO
                    throw new ParseException();
                }
            }
            else
            {
                // TODO
                throw new ParseException();
            }
        }

        if (startPos == -1)
        {
            // TODO
            throw new ParseException();
        }
        else if (curPos < startPos)
        {
            // TODO
            throw new ParseException();
        }

        StringBuilder sb = new StringBuilder();

        for (int i = startPos; i < curPos; i++)
        {
            sb.append(d[i]);
        }

        String s                 = sb.toString();
        float number             = Float.parseFloat(s);
        ParseNumberRetval retval = new ParseNumberRetval();
        retval.number            = number;
        retval.curPos            = curPos;

        return retval;
    }

    public static ParseCommandRetval<SVGDrawingMoveToCommand> ParseMoveToParams(char[] d, int curPos, boolean isRelative) throws ParseException
    {
        ParseNumberRetval parseNumberRetval = null;

        parseNumberRetval = ParseNumber(d, curPos);
        curPos            = parseNumberRetval.curPos;
        float x           = parseNumberRetval.number;

        parseNumberRetval = ParseNumber(d, curPos);
        curPos            = parseNumberRetval.curPos;
        float y           = parseNumberRetval.number;

        SVGDrawingMoveToCommand command = new SVGDrawingMoveToCommand();

        command.isRelative = isRelative;
        command.x          = x;
        command.y          = y;

        ParseCommandRetval<SVGDrawingMoveToCommand> retval = new ParseCommandRetval<>();

        retval.command = command;
        retval.curPos  = curPos;

        return retval;
    }

    public static ParseCommandRetval<SVGDrawingLineToCommand> ParseLineToParams(char[] d, int curPos, boolean isRelative) throws ParseException
    {
        ParseNumberRetval parseNumberRetval = null;

        ParseCommandRetval<SVGDrawingLineToCommand> retval = new ParseCommandRetval<>();

        retval.command = new SVGDrawingLineToCommand();

        retval.command.isRelative = isRelative;

        parseNumberRetval = ParseNumber(d, curPos);
        curPos            = parseNumberRetval.curPos;
        retval.command.x  = parseNumberRetval.number;

        parseNumberRetval = ParseNumber(d, curPos);
        curPos            = parseNumberRetval.curPos;
        retval.command.y  = parseNumberRetval.number;

        retval.curPos = curPos;

        return retval;
    }

    public static ParseCommandRetval<SVGDrawingCurveToCommand> ParseCurveToParams(char[] d, int curPos, boolean isRelative) throws ParseException
    {
        ParseNumberRetval parseNumberRetval = null;

        ParseCommandRetval<SVGDrawingCurveToCommand> retval = new ParseCommandRetval<>();

        retval.command = new SVGDrawingCurveToCommand();

        retval.command.isRelative = isRelative;

        parseNumberRetval = ParseNumber(d, curPos);
        curPos            = parseNumberRetval.curPos;
        retval.command.x1 = parseNumberRetval.number;

        parseNumberRetval = ParseNumber(d, curPos);
        curPos            = parseNumberRetval.curPos;
        retval.command.y1 = parseNumberRetval.number;

        parseNumberRetval = ParseNumber(d, curPos);
        curPos            = parseNumberRetval.curPos;
        retval.command.x2 = parseNumberRetval.number;

        parseNumberRetval = ParseNumber(d, curPos);
        curPos            = parseNumberRetval.curPos;
        retval.command.y2 = parseNumberRetval.number;

        parseNumberRetval = ParseNumber(d, curPos);
        curPos            = parseNumberRetval.curPos;
        retval.command.x  = parseNumberRetval.number;

        parseNumberRetval = ParseNumber(d, curPos);
        curPos            = parseNumberRetval.curPos;
        retval.command.y  = parseNumberRetval.number;

        retval.curPos = curPos;

        return retval;
    }

    public static ParseCommandRetval<SVGDrawingSmoothCurveToCommand> ParseSmoothCurveToParams(char[] d, int curPos, boolean isRelative) throws ParseException
    {
        ParseNumberRetval parseNumberRetval = null;

        ParseCommandRetval<SVGDrawingSmoothCurveToCommand> retval = new ParseCommandRetval<>();

        retval.command = new SVGDrawingSmoothCurveToCommand();

        retval.command.isRelative = isRelative;

        parseNumberRetval = ParseNumber(d, curPos);
        curPos            = parseNumberRetval.curPos;
        retval.command.x2 = parseNumberRetval.number;

        parseNumberRetval = ParseNumber(d, curPos);
        curPos            = parseNumberRetval.curPos;
        retval.command.y2 = parseNumberRetval.number;

        parseNumberRetval = ParseNumber(d, curPos);
        curPos            = parseNumberRetval.curPos;
        retval.command.x  = parseNumberRetval.number;

        parseNumberRetval = ParseNumber(d, curPos);
        curPos            = parseNumberRetval.curPos;
        retval.command.y  = parseNumberRetval.number;

        retval.curPos = curPos;

        return retval;
    }

    public static boolean IsValidNumberStartingChar(char c)
    {
        if (c == '.')
        {
            return true;
        }
        else if (c == '-')
        {
            return true;
        }
        else if ((c >= '0') && (c <= '9'))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public static List<SVGDrawingCommand> ParseSVGPathData(char[] d) throws ParseException
    {
        int curPos                            = 0;
        SVGDrawingCommand lastCommand         = null;
        ParseCommandRetval parseCommandRetval = null;

        ArrayList<SVGDrawingCommand> drawingCommands = new ArrayList<SVGDrawingCommand>();

        while (curPos < d.length)
        {
            char c = d[curPos];
            curPos++;

            if (d[curPos] == 'm')
            {
                // relative `moveto` command
                curPos++;
                if (!(curPos < d.length))
                {
                    // TODO
                    throw new ParseException();
                }

                parseCommandRetval = ParseMoveToParams(d, curPos, true);
                curPos             = parseCommandRetval.curPos;
                lastCommand        = parseCommandRetval.command;
                drawingCommands.add(lastCommand);
            }
            else if (d[curPos] == 'M')
            {
                // absolute `moveto` command
                curPos++;
                if (!(curPos < d.length))
                {
                    // TODO
                    throw new ParseException();
                }

                parseCommandRetval = ParseMoveToParams(d, curPos, false);
                curPos             = parseCommandRetval.curPos;
                lastCommand        = parseCommandRetval.command;
                drawingCommands.add(lastCommand);
            }
            else if (d[curPos] == 'l')
            {
                // relative `lineto` command
                curPos++;
                if (!(curPos < d.length))
                {
                    // TODO
                    throw new ParseException();
                }

                parseCommandRetval = ParseLineToParams(d, curPos, true);
                curPos             = parseCommandRetval.curPos;
                lastCommand        = parseCommandRetval.command;
                drawingCommands.add(lastCommand);
            }
            else if (d[curPos] == 'L')
            {
                // absolute `lineto` command
                curPos++;
                if (!(curPos < d.length))
                {
                    // TODO
                    throw new ParseException();
                }

                parseCommandRetval = ParseLineToParams(d, curPos, false);
                curPos             = parseCommandRetval.curPos;
                lastCommand        = parseCommandRetval.command;
                drawingCommands.add(lastCommand);
            }
            else if (d[curPos] == 'c')
            {
                // relative `curveto` command
                curPos++;
                if (!(curPos < d.length))
                {
                    // TODO
                    throw new ParseException();
                }

                parseCommandRetval = ParseCurveToParams(d, curPos, true);
                curPos             = parseCommandRetval.curPos;
                lastCommand        = parseCommandRetval.command;
                drawingCommands.add(lastCommand);
            }
            else if (d[curPos] == 'C')
            {
                // absolute `curveto` command
                curPos++;
                if (!(curPos < d.length))
                {
                    // TODO
                    throw new ParseException();
                }

                parseCommandRetval = ParseCurveToParams(d, curPos, false);
                curPos             = parseCommandRetval.curPos;
                lastCommand        = parseCommandRetval.command;
                drawingCommands.add(lastCommand);
            }
            else if (d[curPos] == 's')
            {
                // relative `smoothcurveto` command
                curPos++;
                if (!(curPos < d.length))
                {
                    // TODO
                    throw new ParseException();
                }

                parseCommandRetval = ParseSmoothCurveToParams(d, curPos, true);
                curPos             = parseCommandRetval.curPos;
                lastCommand        = parseCommandRetval.command;
                drawingCommands.add(lastCommand);
            }
            else if (d[curPos] == 'S')
            {
                // absolute `smoothcurveto` command
                curPos++;
                if (!(curPos < d.length))
                {
                    // TODO
                    throw new ParseException();
                }

                parseCommandRetval = ParseSmoothCurveToParams(d, curPos, false);
                curPos             = parseCommandRetval.curPos;
                lastCommand        = parseCommandRetval.command;
                drawingCommands.add(lastCommand);
            }
            else if (IsValidNumberStartingChar(d[curPos]))
            {
                if (lastCommand == null)
                {
                    // TODO
                    throw new ParseException();
                }
                else if (lastCommand instanceof SVGDrawingCurveToCommand)
                {
                    parseCommandRetval = ParseCurveToParams(d, curPos, ((SVGDrawingCurveToCommand) lastCommand).isRelative);
                }
                else if (lastCommand instanceof SVGDrawingLineToCommand)
                {
                    parseCommandRetval = ParseLineToParams(d, curPos, ((SVGDrawingLineToCommand) lastCommand).isRelative);
                }
                else if (lastCommand instanceof SVGDrawingMoveToCommand)
                {
                    parseCommandRetval = ParseMoveToParams(d, curPos, ((SVGDrawingMoveToCommand) lastCommand).isRelative);
                }
                else if (lastCommand instanceof SVGDrawingSmoothCurveToCommand)
                {
                    parseCommandRetval = ParseSmoothCurveToParams(d, curPos, ((SVGDrawingSmoothCurveToCommand) lastCommand).isRelative);
                }
                else
                {
                    // TODO
                    throw new ParseException();
                }

                curPos      = parseCommandRetval.curPos;
                lastCommand = parseCommandRetval.command;
                drawingCommands.add(lastCommand);
            }
            else
            {
                // TODO
                throw new ParseException();
            }
        }

        return drawingCommands;
    }
}
