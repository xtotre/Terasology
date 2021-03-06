package org.terasology.math;

import org.terasology.utilities.MathHelper;

import java.util.EnumMap;

/**
 * The six sides of a block.
 */
public enum Side {
    TOP(Vector3i.up(), false),
    LEFT(new Vector3i(-1, 0, 0), true),
    RIGHT(new Vector3i(1, 0, 0), true),
    FRONT(new Vector3i(0, 0, -1), true),
    BACK(new Vector3i(0, 0, 1), true),
    BOTTOM(Vector3i.down(), false);

    private static EnumMap<Side, Side> reverseMap;
    private static Side[] horizontalSides;
    private static EnumMap<Side, Side> clockwiseSide;
    private static EnumMap<Side, Side> antiClockwiseSide;

    static {
        reverseMap = new EnumMap<Side, Side>(Side.class);
        reverseMap.put(TOP, BOTTOM);
        reverseMap.put(LEFT, RIGHT);
        reverseMap.put(RIGHT, LEFT);
        reverseMap.put(FRONT, BACK);
        reverseMap.put(BACK, FRONT);
        reverseMap.put(BOTTOM, TOP);

        clockwiseSide = new EnumMap<Side, Side>(Side.class);
        clockwiseSide.put(Side.FRONT, Side.RIGHT);
        clockwiseSide.put(Side.RIGHT, Side.BACK);
        clockwiseSide.put(Side.BACK, Side.LEFT);
        clockwiseSide.put(Side.LEFT, Side.FRONT);
        clockwiseSide.put(Side.TOP, Side.TOP);
        clockwiseSide.put(Side.BOTTOM, Side.BOTTOM);
        antiClockwiseSide = new EnumMap<Side, Side>(Side.class);
        antiClockwiseSide.put(Side.FRONT, Side.LEFT);
        antiClockwiseSide.put(Side.RIGHT, Side.FRONT);
        antiClockwiseSide.put(Side.BACK, Side.RIGHT);
        antiClockwiseSide.put(Side.LEFT, Side.BACK);
        antiClockwiseSide.put(Side.TOP, Side.TOP);
        antiClockwiseSide.put(Side.BOTTOM, Side.BOTTOM);

        horizontalSides = new Side[]{LEFT, RIGHT, FRONT, BACK};
    }

    /**
     * @return The horizontal sides, for iteration
     */
    public static Side[] horizontalSides() {
        return horizontalSides;
    }

    public static Side inDirection(int x, int y, int z) {
        if (MathHelper.fastAbs(x) > MathHelper.fastAbs(y)) {
            if (MathHelper.fastAbs(x) > MathHelper.fastAbs(z)) {
                return (x > 0) ? RIGHT : LEFT;
            }
        } else if (MathHelper.fastAbs(y) > MathHelper.fastAbs(z)) {
            return (y > 0) ? TOP : BOTTOM;
        }
        return (z > 0) ? BACK : FRONT;
    }

    public static Side inDirection(double x, double y, double z) {
        if (MathHelper.fastAbs(x) > MathHelper.fastAbs(y)) {
            if (MathHelper.fastAbs(x) > MathHelper.fastAbs(z)) {
                return (x > 0) ? RIGHT : LEFT;
            }
        } else if (MathHelper.fastAbs(y) > MathHelper.fastAbs(z)) {
            return (y > 0) ? TOP : BOTTOM;
        }
        return (z > 0) ? BACK : FRONT;
    }

    private Vector3i vector3iDir;
    private boolean horizontal;

    Side(Vector3i vector3i, boolean horizontal) {
        this.vector3iDir = vector3i;
        this.horizontal = horizontal;
    }

    /**
     * @return The vector3i in the direction of the side. Do not modify.
     */
    public Vector3i getVector3i() {
        return vector3iDir;
    }

    /**
     * @return Whether this is one of the horizontal directions.
     */
    public boolean isHorizontal() {
        return horizontal;
    }

    /**
     * @return The opposite side to this side.
     */
    public Side reverse() {
        return reverseMap.get(this);
    }

    public Side rotateClockwise(int steps) {
        if (!isHorizontal()) return this;
        if (steps < 0) {
            steps = -steps + 2;
        }
        steps = steps % 4;
        switch (steps) {
            case 1:
                return clockwiseSide.get(this);
            case 2:
                return reverseMap.get(this);
            case 3:
                return antiClockwiseSide.get(this);
            default:
                return this;
        }
    }

}
