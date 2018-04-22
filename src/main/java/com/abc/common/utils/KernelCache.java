package com.abc.common.utils;


import java.io.Serializable;

/**
 * This is from Smile lib without comments/documentation.
 * Essentially a cache to store the correlation between Documents in the Support Vectors list.
 * Each field either correspond to Cross-Correlation or Auto Correlation.
 */
public final class KernelCache implements Serializable {


    private double[] data;
    private int size;

    public KernelCache() {
        this(10);
    }

    public KernelCache(int capacity) {
        data = new double[capacity];
        size = 0;
    }

    public KernelCache(double[] values) {
        this(Math.max(values.length, 10));
        add(values);
    }


    public void ensureCapacity(int capacity) {
        if (capacity > data.length) {

            double[] tmp = new double[Math.max(data.length << 1, capacity)];
            System.arraycopy(data, 0, tmp, 0, data.length);
            data = tmp;
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void trimToSize() {
        if (data.length > size()) {
            double[] tmp = toArray();
            data = tmp;
        }
    }

    public void add(double val) {
        ensureCapacity(size + 1);
        data[size++] = val;
    }

    public void add(double[] vals) {
        ensureCapacity(size + vals.length);
        System.arraycopy(vals, 0, data, size, vals.length);
        size += vals.length;
    }

    public double get(int index) {
        return data[index];
    }

    public KernelCache set(int index, double val) {

        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException(String.valueOf(index));

        data[index] = val;
        return this;
    }

    public void clear() {
        size = 0;
    }

    /**
     * To Removing an element in the Cache. One of the coolest code piece.
     * @param index
     * @return
     */
    public double remove(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException(String.valueOf(index));


        double old = get(index);
        
        if (index == 0) {
            // data at the front
            System.arraycopy(data, 1, data, 0, size - 1);
        } else if (size - 1 == index) {
            // no copy to make, decrementing pos "deletes" values at
            // the end
        } else {
            // data in the middle
            System.arraycopy(data, index + 1, data, index, size - (index + 1));
        }
        
        size--;
        return old;
    }

    public double[] toArray() {
        return toArray(null);
    }

    public double[] toArray(double[] dest) {

        dest =  dest == null || dest.length < size() ? new double[size] : dest;
        System.arraycopy(data, 0, dest, 0, size);
        return dest;
    }
}