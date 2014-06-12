package com.codexperiments.leakeeper;

/**
 * Contains the information to store the Id of an emitter. Emitter class is necessary since if internal Ids may be quite
 * common and thus similar between emitters of different types (e.g. fragments which have integer Ids starting from 0).
 */
final class TaskEmitterId {
    private final Class<?> mType;
    private final Object mId;

    public TaskEmitterId(Class<?> pType, Object pId) {
        super();
        mType = pType;
        mId = pId;
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) return true;
        if (pOther == null) return false;
        if (getClass() != pOther.getClass()) return false;

        TaskEmitterId lOther = (TaskEmitterId) pOther;
        if (mId == null) {
            if (lOther.mId != null) return false;
        } else if (!mId.equals(lOther.mId)) return false;

        if (mType == null) return lOther.mType == null;
        else return mType.equals(lOther.mType);
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
        return "TaskEmitterId [mType=" + mType + ", mId=" + mId + "]";
    }
}
