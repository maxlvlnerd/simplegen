package com.github.maxlvlnerd.simplegen;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public record Spline(float @NotNull [] keys, float @NotNull [] values) {
    public Spline {
        assert isSorted(keys);
        assert isSorted(values);
        assert keys.length == values.length;
    }

    private static boolean isSorted(float[] arr) {
        var sorted = arr.clone();
        Arrays.sort(sorted);
        return Arrays.equals(sorted, arr);
    }

    private static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    private static float proximity(float a, float b, float x) {
        return (x - a) / (b - a);
    }

    // finds the index of the last closest value to x in keys
    // i.e arr = [0, 0.5, 1], v = 0.6 will return 0.5, v = 0.4 will return 0
    public static int last_without_going_over(float[] arr, float v) {
        var index = 0;
        for (int i = 0; i < arr.length; i++) {
            var k = arr[i];
            if (k > v) break;
            else index = i;
        }

        return index;
    }

    public float interp(FloatFunction easing, float x) {
        // ensure in range
        if (x < this.keys[0] || x > this.keys[this.keys.length - 1]) {
            throw new IllegalArgumentException("x (%s) is outside the range of values [%s,%s]".formatted(x, this.keys[0], this.keys[this.keys.length - 1]));
        }
        var i = last_without_going_over(keys, x);
        if (i + 1 >= keys.length) return values[values.length - 1];

        var t = proximity(keys[i], keys[i + 1], x);
        var a = values[i];
        var b = values[i + 1];
        return lerp(a, b, easing.apply(t));
    }

    @FunctionalInterface
    public interface FloatFunction {
        float apply(float x);
    }
}
