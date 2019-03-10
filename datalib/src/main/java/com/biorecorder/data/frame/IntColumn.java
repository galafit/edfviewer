package com.biorecorder.data.frame;

import com.biorecorder.data.aggregation.AggregateFunction;
import com.biorecorder.data.aggregation.IntAggFunction;
import com.biorecorder.data.list.IntArrayList;
import com.biorecorder.data.sequence.IntSequence;
import com.biorecorder.data.sequence.PrimitiveUtils;
import com.biorecorder.data.sequence.SequenceUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by galafit on 15/1/19.
 */
public class IntColumn implements Column {
    protected final static int NAN = Integer.MAX_VALUE;
    protected final static DataType dataType = DataType.INT;
    protected IntSequence dataSequence;
    protected Stats stats = new Stats();

    public IntColumn(IntSequence data) {
        this.dataSequence = data;
    }

    public IntColumn(int[] data) {
        this(new IntSequence() {
            @Override
            public int size() {
                return data.length;
            }

            @Override
            public int get(int index) {
                return data[index];
            }
        });
    }

    public IntColumn(List<Integer> data) {
        this(new IntSequence() {
            @Override
            public int size() {
                return data.size();
            }

            @Override
            public int get(int index) {
                return data.get(index);
            }
        });
    }

    @Override
    public int size() {
        return dataSequence.size();
    }

    @Override
    public double value(int index) {
        int value = dataSequence.get(index);
        if(value == NAN) {
            return Double.NaN;
        }
        return value;
    }

    @Override
    public String label(int index) {
        int value = dataSequence.get(index);
        if(value == NAN) {
            return null;
        }
        return Integer.toString(value);
    }

    @Override
    public DataType dataType() {
        return dataType;
    }

    @Override
    public Column view(int from, int length) {
        IntSequence subSequence = new IntSequence() {
            @Override
            public int size() {
                if(length < 0) {
                    return dataSequence.size() - from;
                }
                return length;
            }

            @Override
            public int get(int index) {
                return dataSequence.get(index + from);
            }
        };
        return new IntColumn(subSequence);
    }

    @Override
    public Column view(int[] order) {
        IntSequence subSequence = new IntSequence() {
            @Override
            public int size() {
                return order.length;
            }

            @Override
            public int get(int index) {
                return dataSequence.get(order[index]);
            }
        };
        return new IntColumn(subSequence);
    }


    @Override
    public Column slice(int from, int length) {
        int[] slicedData = new int[length];
        for (int i = 0; i < length; i++) {
            slicedData[i] = dataSequence.get(from + i);
        }
        return new IntColumn(slicedData);
    }

    @Override
    public void cache(int nLastChangeable) {
        if(! (dataSequence instanceof IntCachingSequence)) {
            dataSequence = new IntCachingSequence(dataSequence, nLastChangeable);
        }
    }

    @Override
    public void disableCaching() {
        if(dataSequence instanceof IntCachingSequence) {
            dataSequence = ((IntCachingSequence) dataSequence).getInnerData();
        }
    }

    @Override
    public int bisect(double value, int from, int length) {
        return SequenceUtils.bisect(dataSequence, PrimitiveUtils.doubleToInt(value), from,  length);
    }


    @Override
    public StatsInfo stats(int length, int nLastChangeable) {
        return stats.getStats(length, nLastChangeable);
    }

    @Override
    public int[] sort(int from, int length, boolean isParallel) {
        return SequenceUtils.sort(dataSequence, from, length, isParallel);
    }

    @Override
    public IntSequence group(double interval) {
        int sequenceSize = dataSequence.size();

        IntSequence groupIndexes = new IntSequence() {
            int intervalValue = (int) interval;
            IntArrayList groupIndexesList = new IntArrayList(sequenceSize);
            @Override
            public int size() {
                update();
                return groupIndexesList.size();
            }

            @Override
            public int get(int index) {
                return groupIndexesList.get(index);
            }

            private void update() {
                int dataSize =  dataSequence.size();
                int groupListSize =  groupIndexesList.size();
                if(dataSize == 0 || (groupListSize > 0 && groupIndexesList.get(groupListSize - 1) == dataSize)) {
                    return;
                }

                int from;
                if(groupListSize == 0) {
                    groupIndexesList.add(0);
                    from = 0;
                } else {
                    // delete last "closing" group
                    groupIndexesList.remove(groupListSize - 1);
                    from = groupIndexesList.get(groupListSize - 2);
                }

                int groupValue = ((dataSequence.get(from) / intervalValue)) * intervalValue;
                groupValue += intervalValue;
                for (int i = from + 1;  i < dataSize; i++) {
                    int data = dataSequence.get(i);
                    if (dataSequence.get(i) >= groupValue) {
                        groupIndexesList.add(i);
                        groupValue += intervalValue; // often situation

                        if(data > groupValue) { // rare situation
                            groupValue = ((dataSequence.get(i) / intervalValue)) * intervalValue;
                            groupValue += intervalValue;
                        }
                    }
                }
                // add last "closing" groupByEqualIntervals
                groupIndexesList.add(dataSize);
            }
        };
        return groupIndexes;
    }

    @Override
    public Column aggregate(AggregateFunction aggregateFunction, IntSequence groupIndexes) {
        IntSequence resultantSequence = new IntSequence() {
            private IntAggFunction aggFunction = (IntAggFunction) aggregateFunction.getFunctionImpl(dataType);;
            private int lastIndex = -1;

            @Override
            public int size() {
                return groupIndexes.size() - 1;
            }

            @Override
            public int get(int index) {
                if(index != lastIndex) {
                    aggFunction.reset();
                    lastIndex = index;
                }
                int n = aggFunction.getN();
                int length = groupIndexes.get(index + 1) - groupIndexes.get(index) - n;
                int from = groupIndexes.get(index) + n;
                if(length > 0) {
                    aggFunction.add(dataSequence, from, length);
                }
                return aggFunction.getValue();
            }
        };
        return new IntColumn(resultantSequence);
    }

    class Stats {
        protected int count;
        private int min;
        private int max;
        private boolean isIncreasing = true;
        private boolean isDecreasing = true;

        public StatsInfoInt calculate(int from, int length) {
           int min1 = dataSequence.get(from);
           int max1 = min1;
           boolean isIncreasing1 = true;
           boolean isDecreasing1 = true;

            for (int i = 1; i < length; i++) {
              min1 = Math.min(min1, dataSequence.get(i + from));
              max1 = Math.max(max1, dataSequence.get(i + from));
              if(isIncreasing1 || isDecreasing1) {
                  int diff = dataSequence.get(i + from) - dataSequence.get(i + from - 1);
                  if(isDecreasing1 && diff > 0) {
                      isDecreasing1 = false;
                  }
                  if(isIncreasing1 && diff < 0) {
                      isIncreasing1 = false;
                  }
              }
            }

           return new StatsInfoInt(min1, max1, isIncreasing1, isDecreasing1);
        }

        public StatsInfoInt getStats(int length, int nLastChangeable) {
            if(length <= 0) {
                String errMsg = "Statistic can not be calculated if length <= 0: "+length;
                throw new IllegalStateException(errMsg);
            }

            int length1 = length - nLastChangeable;
            if(length1 <= 0) {
                return calculate(0, nLastChangeable);
            }
            if(length1 < count) {
                count = 0;
            }
            if(length1 > count) {
                StatsInfoInt stats = calculate(count, length1 - count);
                if(count == 0) {
                    min = stats.getMin();
                    max = stats.getMax();
                    isIncreasing = stats.isIncreasing();
                    isDecreasing = stats.isDecreasing();
                    count = length1;
                } else {
                    min = Math.min(min, stats.getMin());
                    max = Math.max(max, stats.getMax());
                    isIncreasing = isIncreasing && stats.isIncreasing();
                    isDecreasing = isDecreasing && stats.isDecreasing();
                    count = length1;
                }
            }

            if(nLastChangeable > 0) {
                StatsInfoInt stats = calculate(length1, nLastChangeable);
                return new StatsInfoInt(Math.min(min, stats.getMin()), Math.max(max, stats.getMax()), isIncreasing && stats.isIncreasing(), isDecreasing && stats.isDecreasing());
            } else {
                return new StatsInfoInt(min, max, isIncreasing, isDecreasing);
            }

        }
    }

    class StatsInfoInt implements StatsInfo {
        private final int min;
        private final int max;
        private final boolean isIncreasing;
        private final boolean isDecreasing;

        public StatsInfoInt(int min, int max, boolean isIncreasing, boolean isDecreasing) {
            this.min = min;
            this.max = max;
            this.isIncreasing = isIncreasing;
            this.isDecreasing = isDecreasing;
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        @Override
        public double min() {
            return min;
        }

        @Override
        public double max() {
            return max;
        }

        @Override
        public boolean isIncreasing() {
            return isIncreasing;
        }

        @Override
        public boolean isDecreasing() {
            return isDecreasing;
        }
    }

    public static void main(String [ ] args) {
        Integer[] data = {5, 6, 2, 1, 20, 15, 34, -10};
        List<Integer> dataList = new ArrayList<Integer>(Arrays.asList(data));
        IntColumn column = new IntColumn(dataList);

        StatsInfo stats = column.stats(8, 1);
        System.out.println( stats.min() + " min max "+stats.max());

        dataList.set(7, 40);

        stats = column.stats(8, 1);
        System.out.println( stats.min() + " min max "+stats.max());

        stats = column.stats(3, 1);
        System.out.println( stats.min() + " min max "+stats.max());

    }
}
