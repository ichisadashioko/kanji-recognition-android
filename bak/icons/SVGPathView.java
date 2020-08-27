package io.github.ichisadashioko.android.kanji.views.icons;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

public class SVGPathView extends View {
    public String svgPathData;
    public Path svgPath;
    public int backgroundColor;
    public int fillColor;
    public int strokeColor;
    public Rect svgViewBox;

    public Paint backgroundPaint;

    public void initStrokeColor() {
        initFillColor(Color.rgb(255, 255, 255));
    }

    public void initStrokeColor(int color) {
        strokeColor = color;
    }

    public void initFillColor() {
        initFillColor(Color.rgb(255, 255, 255));
    }

    public void initFillColor(int color) {
        fillColor = color;
    }

    public void initBackgroundPaint() {
        initBackgroundPaint(Color.argb(0, 0, 0, 0));
    }

    public void initBackgroundPaint(int color) {
        backgroundColor = color;
        backgroundPaint = new Paint();
        backgroundPaint.setColor(color);
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setAntiAlias(false);
    }

    public void setSvgPathData(String svgPathData) {
        this.svgPathData = svgPathData;

        try {
            drawingCommands = SVGParser.ParseSVGPathData(svgPathData.toCharArray());
        } catch (Exception ex) {
            System.err.println("Failed to parse svg path data string: " + svgPathData);
            ex.printStackTrace();

            drawingCommands = null;
            return;
        }

        if ((drawingCommands == null) || (drawingCommands.length == 0)) {
            // TODO
            System.err.println("Parsing SVG path data failed or invalid!");
            Thread.dumpStack();
        }

        invalidate();
    }

    public static void CompileToAndroidPath(SVGDrawingCommand[] commands, float srcWidth, float srcHeight, float dstWidth, float dstHeight) throws Exception {
        if ((commands == null) || (commands.length == 0)) {
            // TODO
            throw new Exception("Invalid parameter!");
        }

        float widthScale = dstWidth / srcWidth;
        float heightScale = dstHeight / srcHeight;
        float scale, offsetX, offsetY;

        if (widthScale > heightScale) {
            scale = heightScale;
        } else {
            scale = widthScale;
        }

        offsetX = dstWidth - (srcWidth * scale);
        offsetY = dstHeight - (srcHeight * scale);

        int commandIdx = 0;
        PointF initialPos = null;
        PointF currentPos = null;
        Path renderedAndroidPath = new Path();

        // the first command has to be the `moveto` command
        SVGDrawingCommand firstCommand = commands[commandIdx];
        commandIdx++;

        if (firstCommand instanceof SVGDrawingMoveToCommand) {
            if (!((SVGDrawingMoveToCommand) firstCommand).isRelative) {
                float x = ScaleCoordinate(((SVGDrawingMoveToCommand) firstCommand).x, scale, offsetX);
                float y = ScaleCoordinate(((SVGDrawingMoveToCommand) firstCommand).y, scale, offsetY);
                initialPos = new PointF(x, y);
                currentPos = new PointF(initialPos.x, initialPos.y);
                renderedAndroidPath.moveTo(currentPos.x, currentPos.y);
            } else {
                // TODO
                throw new Exception("absolute move to command required!");
            }
        } else {
            // TODO
            throw new Exception("The first command is not the `moveto` command!");
        }

        // cached data for curve to commands
        // we initialized variables only in order to able to compile the program. their values are not used.
        float x = 0;
        float y = 0;
        float x1 = 0;
        float y1 = 0;
        float x2 = 0;
        float y2 = 0;

        while (commandIdx < commands.length) {
            SVGDrawingCommand command = commands[commandIdx];
            commandIdx++;

            if (command instanceof SVGDrawingMoveToCommand) {
                SVGDrawingMoveToCommand sc = (SVGDrawingMoveToCommand) command;
                if (sc.isRelative) {
                    x = currentPos.x + ScaleCoordinate(sc.x, scale, offsetX);
                    y = currentPos.y + ScaleCoordinate(sc.y, scale, offsetY);
                } else {
                    x = ScaleCoordinate(sc.x, scale, offsetX);
                    y = ScaleCoordinate(sc.y, scale, offsetY);
                }

                currentPos = new PointF(x, y);
                renderedAndroidPath.moveTo(currentPos.x, currentPos.y);
            } else if (command instanceof SVGDrawingLineToCommand) {
                SVGDrawingLineToCommand sc = (SVGDrawingLineToCommand) command;
                if (sc.isRelative) {
                    x = currentPos.x + ScaleCoordinate(sc.x, scale, offsetX);
                    y = currentPos.y + ScaleCoordinate(sc.y, scale, offsetY);
                    currentPos = new PointF(x, y);
                } else {
                    x = ScaleCoordinate(sc.x, scale, offsetX);
                    y = ScaleCoordinate(sc.y, scale, offsetY);
                }

                currentPos = new PointF(x, y);
                renderedAndroidPath.lineTo(currentPos.x, currentPos.y);
            } else if (command instanceof SVGDrawingCurveToCommand) {
                SVGDrawingCurveToCommand sc = (SVGDrawingCurveToCommand) command;

                if (sc.isRelative) {
                    x = currentPos.x + ScaleCoordinate(sc.x, scale, offsetX);
                    x1 = currentPos.x + ScaleCoordinate(sc.x1, scale, offsetX);
                    x2 = currentPos.x + ScaleCoordinate(sc.x2, scale, offsetX);

                    y = currentPos.y + ScaleCoordinate(sc.y, scale, offsetY);
                    y1 = currentPos.y + ScaleCoordinate(sc.y1, scale, offsetY);
                    y2 = currentPos.y + ScaleCoordinate(sc.y2, scale, offsetY);
                } else {
                    x = ScaleCoordinate(sc.x, scale, offsetX);
                    x1 = ScaleCoordinate(sc.x1, scale, offsetX);
                    x2 = ScaleCoordinate(sc.x2, scale, offsetX);

                    y = ScaleCoordinate(sc.y, scale, offsetY);
                    y1 = ScaleCoordinate(sc.y1, scale, offsetY);
                    y2 = ScaleCoordinate(sc.y2, scale, offsetY);
                }

                renderedAndroidPath.cubicTo(x1, y1, x2, y2, x, y);
                currentPos = new PointF(x, y);
            } else if (command instanceof SVGDrawingSmoothCurveToCommand) {
                SVGDrawingSmoothCurveToCommand sc = (SVGDrawingSmoothCurveToCommand) command;

                SVGDrawingCommand prevCommand = commands[commandIdx - 1];
                if ((prevCommand instanceof SVGDrawingCurveToCommand) || (prevCommand instanceof SVGDrawingSmoothCurveToCommand)) {
                    x1 = x2;
                    y1 = y2;
                } else if (commands[commandIdx - 1] instanceof SVGDrawingSmoothCurveToCommand) {

                } else {
                    x1 = currentPos.x;
                    y1 = currentPos.y;
                }

                if (sc.isRelative) {
                    x = currentPos.x + ScaleCoordinate(sc.x, scale, offsetX);
                    x2 = currentPos.x + ScaleCoordinate(sc.x2, scale, offsetX);

                    y = currentPos.y + ScaleCoordinate(sc.y, scale, offsetY);
                    y2 = currentPos.y + ScaleCoordinate(sc.y2, scale, offsetY);
                } else {
                    x = ScaleCoordinate(sc.x, scale, offsetX);
                    x2 = ScaleCoordinate(sc.x2, scale, offsetX);

                    y = ScaleCoordinate(sc.y, scale, offsetY);
                    y2 = ScaleCoordinate(sc.y2, scale, offsetY);
                }

                renderedAndroidPath.cubicTo(x1, y1, x2, y2, x, y);
                currentPos = new PointF(x, y);
            }else if(command instanceof SVGDrawingClosePathCommand){
                // TODO open new path?
                renderedAndroidPath.close();
            }
        }
    }

    public static float ScaleCoordinate(float originalCoordinate, float scale, float offset) {
        float retval = (originalCoordinate * scale) + offset;
        return retval;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = this.getWidth();
        int height = this.getHeight();

        Rect rect = new Rect(0, 0, width, height);

        if (backgroundPaint == null) {
            initBackgroundPaint();
        }

        canvas.drawRect(rect, backgroundPaint);

        int svgOffsetTop;
        int svgOffsetRight;
        int svgWidth;
        int svgHeight;
    }

    public SVGPathView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
}
