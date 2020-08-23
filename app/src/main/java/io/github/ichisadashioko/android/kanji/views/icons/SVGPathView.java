package io.github.ichisadashioko.android.kanji.views.icons;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

abstract class SVGDrawingCommand
{
}

class SVGDrawingMoveToCommand extends SVGDrawingCommand
{
    public boolean isRelative;
    public float x;
    public float y;
}

class SVGDrawingCurveToCommand
{
    public boolean isRelative;
    public float x1;
    public float y1;
    public float x2;
    public float y2;
    public float x;
    public float y;
}

class SVGDrawingSmoothCurveToCommand
{
    public boolean isRelative;
    public float x2;
    public float y2;
    public float x;
    public float y;
}

class ParseException extends Exception
{
}

class ParseNumberRetval
{
    public int curPos;
    public float number;
}

class ParseCommandRetval<T extends SVGDrawingCommand>
{
    public int curPos;
    public T command;
}

public class SVGPathView extends View
{
    public SVGPathView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

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

        String s = "";

        for (int i = startPos; i < curPos; i++)
        {
            s += d[i];
        }

        float number             = Float.parseFloat(s);
        ParseNumberRetval retval = new ParseNumberRetval();
        retval.number            = number;
        retval.curPos            = curPos;

        return retval;
    }

    public static ParseCommandRetval<SVGDrawingMoveToCommand> ParseRelativeMoveTo(char[] d, int curPos) throws ParseException
    {
        ParseNumberRetval parseNumberRetval = null;

        parseNumberRetval = ParseNumber(d, curPos);
        curPos            = parseNumberRetval.curPos;
        float x           = parseNumberRetval.number;

        parseNumberRetval = ParseNumber(d, curPos);
        curPos            = parseNumberRetval.curPos;
        float y           = parseNumberRetval.number;

        SVGDrawingMoveToCommand command = new SVGDrawingMoveToCommand();

        command.isRelative = true;
        command.x          = x;
        command.y          = y;

        ParseCommandRetval<SVGDrawingMoveToCommand> retval = new ParseCommandRetval<>();

        retval.command = command;
        retval.curPos  = curPos;

        return retval;
    }

    public static void ParseSVGPathData(char[] d) throws ParseException
    {
        int curPos                                   = 0;
        SVGDrawingCommand lastCommand                = null;
        ArrayList<SVGDrawingCommand> drawingCommands = new ArrayList<SVGDrawingCommand>();

        while (curPos < d.length)
        {
            char c = d[curPos];
            curPos++;

            if (d[curPos] == 'm')
            {
                // relative `moveto` command
                curPos++;
            }
        }
    }
}
