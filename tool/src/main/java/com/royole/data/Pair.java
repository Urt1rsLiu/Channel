package com.royole.data;

/**
 * A key-value data structure to record file block content and its offset
 *
 * @author Hongzhi Liu
 * @date 2018/10/9 9:38 
 */
public final class Pair<A, B> {
    private final A key;
    private final B value;

    private Pair(A key, B value) {
        this.key = key;
        this.value = value;
    }

    public static <A, B> Pair<A, B> create(A first, B second) {
        return new Pair<A, B>(first, second);
    }

    public A getFirst() {
        return key;
    }

    public B getSecond() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        @SuppressWarnings("rawtypes")
        Pair other = (Pair) obj;
        if (key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!key.equals(other.getFirst())) {
            return false;
        }
        if (value == null) {
            if (other.getSecond() != null) {
                return false;
            }
        } else if (!value.equals(other.getSecond())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "first = " + key + " , second = " + value;
    }
}
