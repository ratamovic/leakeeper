package com.codexperiments.leakeeper.internal;

/**
 * Contains the information to store the Id of an emitter. Emitter class is necessary since if internal Ids may be quite common
 * and thus similar between emitters of different types (e.g. fragments which have integer Ids starting from 0).
 */
public final class EmitterId {
    private final Class<?> mType;
    private final Object mId;

    public EmitterId(Class<?> pType, Object pId) {
        super();
        mType = pType;
        mId = pId;
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) return true;
        if (pOther == null) return false;
        if (getClass() != pOther.getClass()) return false;

        EmitterId other = (EmitterId) pOther;
        if (mId == null) {
            if (other.mId != null) return false;
        } else if (!mId.equals(other.mId)) return false;

        if (mType == null) return other.mType == null;
        else return mType.equals(other.mType);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mId == null) ? 0 : mId.hashCode());
        result = prime * result + ((mType == null) ? 0 : mType.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "EmitterId [mType=" + mType + ", mId=" + mId + "]";
    }
}