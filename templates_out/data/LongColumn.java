package java.com.biorecorder.data.frame;

import java.com.biorecorder.data.aggregation.AggregateFunction;
import com.biorecorder.basechart.Range;
import java.com.biorecorder.data.list.LongArrayList;
import java.com.biorecorder.data.aggregation.impl.LongAggregateFunction;;
import java.com.biorecorder.data.sequence.LongSequence;
import java.com.biorecorder.data.sequence.LongSequence;
import java.com.biorecorder.data.sequence.SequenceUtils;

import java.util.List;

/**
 * Created by galafit on 27/9/17.
 */
class LongColumn extends NumberColumn {
    protected LongSequence sequence;

    public LongColumn(LongSequence sequence) {
        this.sequence = sequence;
    }

    public LongColumn(long[] data) {
        this(new LongSequence() {
            @Override
            public long size() {
                return data.length;
            }

            @Override
            public long get(long index) {
                return data[(int) index];
            }
        });
    }

    public LongColumn(List<Long> data) {
        this(new LongSequence() {
            @Override
            public long size() {
                return data.size();
            }

            @Override
            public long get(long index) {
                return data.get((int) index);
            }
        });
    }

    @Override
    public void add(double value) throws UnsupportedOperationException {
        if(sequence instanceof LongArrayList) {
            ((LongArrayList) sequence).add((long) value);
        } else {
            throw  new UnsupportedOperationException("Value can be added to the column only if that column wraps ArrayList");
        }
    }

    @Override
    public void remove(int index) {
        if(sequence instanceof LongArrayList) {
            ((LongArrayList) sequence).remove(index);
        } else {
            throw  new UnsupportedOperationException("Value can be removed from the column only if that column wraps ArrayList");
        }
    }

    @Override
    public void add(double[] values) throws UnsupportedOperationException {
        if(sequence instanceof LongArrayList) {
            long[] castedValues = new long[values.length];
            for (int i = 0; i < values.length; i++) {
               castedValues[i] = (long) values[i];
            }
            ((LongArrayList)sequence).add(castedValues);
        } else {
            throw  new UnsupportedOperationException("Values can be added to the column only if that column wraps ArrayList");
        }
    }

    @Override
    public long size() {
        return sequence.size();
    }

    @Override
    public double value(long index) {
        return sequence.get(index);
    }

    @Override
    public NumberColumn subColumn(long fromIndex, long length) {
        LongSequence subSequence = new LongSequence() {
            @Override
            public long size() {
                if(length < 0) {
                    return sequence.size() - fromIndex;
                }
                return length;
            }

            @Override
            public long get(long index) {
                return sequence.get(index + fromIndex);
            }
        };
        LongColumn subColumn = new LongColumn(subSequence);
        subColumn.name = name;
        subColumn.aggregateFunctions = aggregateFunctions;
        return subColumn;
    }

    @Override
    public Range extremes() {
        long size = sequence.size();
        if(size == 0){
            return null;
        }
        if (size > Integer.MAX_VALUE) {
            String errorMessage = "Extremes can not be find if rowCount > Integer.MAX_VALUE. Size = " + size();
            throw new IllegalArgumentException(errorMessage);
        }

        // invoke data.get(i) can be expensive in the case data is grouped data
        long dataItem = sequence.get(0); //
        long min = dataItem;
        long max = dataItem;
        for (long i = 1; i < size ; i++) {
            dataItem = sequence.get(i);
            min = (long)Math.min(min, dataItem);
            max = (long)Math.max(max, dataItem);
        }
        return new Range(min, max);
    }

    @Override
    public long binarySearch(double value) {
        long size = sequence.size();
        if (size > Integer.MAX_VALUE) {
            String errorMessage = "Binary search can not be done if rowCount > Integer.MAX_VALUE. Size = " + size;
            throw new IllegalArgumentException(errorMessage);
        }
        return SequenceUtils.upperBound(sequence, (long) value, 0, (int) size);

    }

    @Override
    public long upperBound(double value) {
        long size = sequence.size();
        if (size > Integer.MAX_VALUE) {
            String errorMessage = "Upper bound binary search can not be done if rowCount > Integer.MAX_VALUE. Size = " + size;
            throw new IllegalArgumentException(errorMessage);
        }
        return SequenceUtils.upperBound(sequence, (long) value, 0, (int) size);
    }

    @Override
    public long lowerBound(double value) {
        long size = sequence.size();
        if (size > Integer.MAX_VALUE) {
            String errorMessage = "Lower bound binary search can not be done if rowCount > Integer.MAX_VALUE. Size = " + size;
            throw new IllegalArgumentException(errorMessage);
        }
        return SequenceUtils.lowerBound(sequence, (long) value, 0, (int) size);
    }

    @Override
    public NumberColumn copy() {
        LongColumn copyColumn = new LongColumn(sequence);
        copyColumn.name = name;
        copyColumn.aggregateFunctions = aggregateFunctions;
        return copyColumn;
    }


    @Override
    public NumberColumn cache() {
        long size = sequence.size();
        if (size > Integer.MAX_VALUE) {
            String errorMessage = "Column can not be cached if its rowCount > Integer.MAX_VALUE. Size = " + size;
            throw new IllegalArgumentException(errorMessage);
        }
        LongArrayList list = new LongArrayList((int) size);
        for (int i = 0; i < size; i++) {
            list.add(sequence.get(i));
        }
        LongColumn cacheColumn = new LongColumn(list);
        cacheColumn.name = name;
        cacheColumn.aggregateFunctions = aggregateFunctions;
        return cacheColumn;
    }

    @Override
    public NumberColumn[] group(LongSequence groupStartIndexes) {
        NumberColumn[] resultantColumns = new NumberColumn[aggregateFunctions.length];

        for (int i = 0; i < aggregateFunctions.length; i++) {
            resultantColumns[i] = new LongColumn(new GroupedSequence(aggregateFunctions[i], groupStartIndexes));
            String resultantName = name;
            if(aggregateFunctions.length > 1) {
                resultantName = name + " "+aggregateFunctions[i].name();
            }
            resultantColumns[i].setName(resultantName);
            resultantColumns[i].setAggregateFunctions(aggregateFunctions[i]);
        }
        return resultantColumns;
    }

    class GroupedSequence implements LongSequence {
        private LongSequence groupStartIndexes;
        private long lastGroupValueStart = -1;
        private long lastGroupValueLength;

        private final LongAggregateFunction aggregateFunction;

        public GroupedSequence(AggregateFunction aggregateFunction, LongSequence groupStartIndexes) {
            this.groupStartIndexes = groupStartIndexes;
            this.aggregateFunction = (LongAggregateFunction) aggregateFunction.getFunctionImpl("long");
        }

        @Override
        public long size() {
            return groupStartIndexes.size() - 1;
        }

        @Override
        public long get(long index) {
            if(lastGroupValueStart != groupStartIndexes.get(index)) {
                aggregateFunction.reset();
                lastGroupValueLength = 0;
            }

            long groupEnd = Math.min(groupStartIndexes.get(index + 1), sequence.size());

            long groupValue = aggregateFunction.addToGroup(sequence, groupStartIndexes.get(index) + lastGroupValueLength, groupEnd - groupStartIndexes.get(index) - lastGroupValueLength);

            lastGroupValueStart = groupStartIndexes.get(index);
            lastGroupValueLength = groupEnd - groupStartIndexes.get(index);
            return groupValue;
        }
    }
}
