package com.dfrobot.angelo.blunobasicdemo;

import android.graphics.Canvas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.util.AttributeSet;

import com.jjoe64.graphview.series.DataPoint;

public class DrawView extends View
{
    private Paint paint = new Paint();
    private float xPos, yPos;
    private Line3D line = new Line3D(200, 0, 0);

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(15);

    }

    public void translate(double x, double y)
    {
        xPos += x;
        yPos += y;
    }

    int arrowHeadLen = 40;
    int radius = 32;
    @Override
    public void onDraw(Canvas canvas)
    {
        canvas.drawOval((100+xPos-radius), (100+yPos+radius), (100+xPos+radius), (100+yPos-radius), paint);

        /*
        DataPoint start = line.getStartPos();
        DataPoint end = line.getEndPos();
        canvas.drawLine((int)(100 + start.getX()), (int)(100 + start.getY()),
                (int)(100 + end.getX()), (int)(100 + end.getY()), paint);
        double arrowHeadAngle = Math.atan2(start.getY(), start.getX());
        canvas.drawLine((int)(100 + start.getX()), (int)(100 + start.getY()),
                (int)(100+start.getX() + arrowHeadLen * Math.cos(arrowHeadAngle + 5*Math.PI / 6)),
                (int)(100+start.getY() + arrowHeadLen * Math.sin(arrowHeadAngle + 5*Math.PI / 6)), paint);
        canvas.drawLine((int)(100 + start.getX()), (int)(100 + start.getY()),
                (int)(100+start.getX() + arrowHeadLen * Math.cos(arrowHeadAngle - 5*Math.PI / 6)),
                (int)(100+start.getY() + arrowHeadLen * Math.sin(arrowHeadAngle - 5*Math.PI / 6)), paint);*/

        /*
        int arrowHeadLen = 40;
        canvas.drawLine((int)(100 + start.getX()), (int)(start.getY() + 100),
                (int)(100 + start.getX() + arrowHeadLen * Math.cos(line.angleX + 5*Math.PI/6) * Math.sin(line.angleY)),
                (int)(100 + start.getY() + arrowHeadLen * Math.sin(line.angleX) + 5*Math.PI/6), paint);
        /*
        canvas.drawLine((int)(100 + start.getX()), (int)(start.getY() + 100),
                (int)(100 + start.getX() + arrowHeadLen * Math.cos(line.angleX + 7*Math.PI/6)),
                (int)(100 + start.getY() + arrowHeadLen * Math.sin(line.angleX + 7*Math.PI/6)), paint);*/
    }

    public void setAngles(double angleX, double angleY)
    {
        line.angleX = angleX;
        line.angleY = angleY;
    }

    public void rotate(double deltaXAngle, double deltaYAngle)
    {
        line.angleX += deltaXAngle;
        line.angleY += deltaYAngle;
    }

    public double getAngleX()
    {
        return line.angleX;
    }

    public double getAngleY()
    {
        return line.angleY;
    }

    public class Line3D
    {
        double length = 0;
        double angleX = 0,
                angleY = 0;

        Line3D(double len, double startAngleX, double startAngleY)
        {
            this.length = len;
            this.angleX = startAngleX;
            this.angleY = startAngleY;
        }

        private DataPoint getStartPos ()
        {
            return new DataPoint(length/2 * Math.cos(angleX) * Math.sin(angleY), length / 2 * Math.sin(angleX));
        }

        private DataPoint getEndPos ()
        {
            DataPoint start = getStartPos();
            return new DataPoint(-start.getX(), -start.getY());
        }
    }
}
