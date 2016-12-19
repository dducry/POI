package com.poi.poi;

/**
 * Created by florian on 19/12/16.
 */


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;

import java.util.LinkedList;

/**
 * Created by florian on 19/12/16.
 */

public class HUDView extends View {
    Paint paint = new Paint();


    LinkedList<Float> az = new LinkedList<Float>();
    LinkedList<Float> pt = new LinkedList<Float>();
    LinkedList<Float> rl = new LinkedList<Float>();

    public float targetPitch = -45;
    public float targetAzimuth = 179;

    float hFov = 100;
    float vFov = 100;

    public HUDView(Context context, float hFov, float vFov) {
        super(context);
        paint.setColor(0xff00ff00);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setAntiAlias(true);

        this.hFov = hFov;
        this.vFov = vFov;

    };


    private float mean(LinkedList<Float> l)
    {
        float sum = 0;
        for(Float f : l)
        {
            sum += f;
        }
        return sum / l.size();
    }

    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        int centerx = width/2;
        int centery = height/2;
        //canvas.drawLine(centerx, 0, centerx, height, paint);
        //canvas.drawLine(0, centery, width, centery, paint);
        // Rotate the canvas with the azimut
            /*canvas.rotate(-orientation[0]*360/(2*3.14159f), centerx, centery);*/
        paint.setColor(0xff0000ff);
        float meanAz = mean(az)*180/3.14259f;
        float meanPt = mean(pt)*180/3.14259f;
        float meanRl = mean(rl)*180/3.14259f;

        float deltaAzimuth = meanAz - targetAzimuth;
        while(deltaAzimuth > 180)
            deltaAzimuth -= 360;
        while(deltaAzimuth < -180)
            deltaAzimuth += 360;
        float deltaPitch = meanPt - targetPitch;

        float deltaX = -(width / 2)*(deltaAzimuth/(hFov/2));
        float deltaY = -(height / 2)*(deltaPitch/(vFov/2));

        if(Math.abs(deltaX) < width/2)
            canvas.drawLine(centerx+deltaX, -1000, centerx+deltaX, +1000, paint);
        else {
            int dir = 1;
            if(deltaX < 0)
                dir = -1;
            paint.setColor(0xffFF0000);
            canvas.drawLine(centerx + (dir*width/4), centery, centerx+(dir*width/2), centery, paint);
            paint.setColor(0xff0000ff);
        }
        if(Math.abs(deltaY) < height/2)
            canvas.drawLine(-1000, centery+deltaY, +1000, centery+deltaY, paint);
        else {
            int dir = 1;
            if(deltaY < 0)
                dir = -1;
            paint.setColor(0xffFF0000);
            canvas.drawLine(centerx, centery + (dir*height/4), centerx, centery+(dir*height/2), paint);
            paint.setColor(0xff0000ff);
        }
        //canvas.drawLine(-1000, centery, 1000, centery, paint);
        canvas.drawText("Azimuth" + meanAz, centerx, centery-10, paint);
        canvas.drawText("Pitch" + meanPt, centerx, centery+15, paint);
        canvas.drawText("Roll" + meanRl, centerx, centery+35, paint);
        paint.setColor(0xff00ff00);
    }
}

