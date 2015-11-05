package com.sss.magicwheel.util;

import android.graphics.Point;
import android.util.Log;
import android.view.Display;

/**
 * @author Alexey
 * @since 05.11.2015
 */
public class MagicCalculationHelper {

    public static final int SEGMENT_ANGULAR_HEIGHT = 30;

    public static final double FROM_RADIAN_TO_GRAD_COEF = 180 / Math.PI;

    private static final String TAG = MagicCalculationHelper.class.getCanonicalName();
    private static MagicCalculationHelper instance;

    private int screenWidth;
    private int screenHeight;

    private int innerRadius;
    private int outerRadius;

    private MagicCalculationHelper(Display display) {
        calcScreenSize(display);
        calcCircleDimens();
        instance = this;
    }

    public static MagicCalculationHelper getInstance() {
        if (instance == null) {
            throw new IllegalStateException("initialize() has not been invoked yet.");
        }
        return instance;
    }

    public static void initialize(Display display) {
        instance = new MagicCalculationHelper(display);
    }


    // ----------------------------------------------------

    public static double fromRadToGrad(double valInRad) {
        return valInRad * FROM_RADIAN_TO_GRAD_COEF;
    }

    public CoordinateHolder toScreenCoordinates(CoordinateHolder from) {
        return new CoordinateHolder(from.getX(), screenHeight / 2 - from.getY());
    }

    private void calcScreenSize(Display display) {
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
        Log.e(TAG, String.format("screenWidth [%s], screenHeight [%s]", screenWidth, screenHeight));
    }

    private void calcCircleDimens() {
        innerRadius = screenHeight / 2 + 50;
        outerRadius = innerRadius + 200;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public int getInnerRadius() {
        return innerRadius;
    }

    public int getOuterRadius() {
        return outerRadius;
    }

    public int getAngularHeight() {
        return SEGMENT_ANGULAR_HEIGHT;
    }

    public Point getCircleCenter() {
        return new Point(0, screenHeight / 2);
    }

    public double getStartAngle() {
        int halfScreen = screenHeight / 2;
        double x = Math.sqrt(innerRadius * innerRadius - halfScreen * halfScreen);
        return Math.atan(halfScreen/x);
    }

    public CoordinateHolder getStartIntersectForInnerRadius() {
        return CoordinateHolder.ofPolar(innerRadius, getStartAngle());
    }

    public CoordinateHolder getStartIntercectForOuterRadius() {
        return CoordinateHolder.ofPolar(outerRadius, getStartAngle());
    }



    public static class CoordinateHolder {

        private final double x;
        private final double y;

        public CoordinateHolder(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public static CoordinateHolder ofPolar(double radius, double angle) {
            return new CoordinateHolder(radius * Math.cos(angle), radius * Math.sin(angle));
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        @Override
        public String toString() {
            return String.format("CoordinateHolder (x, y) [%s; %s]", x, y);
        }
    }
}