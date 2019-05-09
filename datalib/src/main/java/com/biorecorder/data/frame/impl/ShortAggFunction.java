package com.biorecorder.data.frame.impl;

import com.biorecorder.data.sequence.ShortSequence;

 /**************************************
  * This file is automatically created.
  * DO NOT MODIFY IT!
  * Edit template file _E_AggFunction.tmpl
  *************************************/

abstract class  ShortAggFunction {
    protected int count;

    public abstract int add(ShortSequence sequence, int from, int length);

    /**
     * get value without checkIfEmpty
     */
    protected abstract short getValue1();

    public final short getValue() {
        checkIfEmpty();
        return getValue1();
    }

    public int getN() {
        return count;
    }

    public void reset() {
            count = 0;
    }

    private void checkIfEmpty() {
        if(count == 0) {
            String errMsg = "No elements was added to group. Grouping function can not be calculated.";
            throw new IllegalStateException(errMsg);
        }
    }
}
