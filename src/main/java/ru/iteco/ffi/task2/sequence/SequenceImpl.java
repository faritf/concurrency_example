package ru.iteco.ffi.task2.sequence;

import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Non-blocking threadsafe BigInteger sequence
 */
public class SequenceImpl implements Sequence {
    private final AtomicReference<BigInteger> value = new AtomicReference<>();
    private final BigInteger increment;

    public SequenceImpl(BigInteger initialValue, BigInteger increment) {
        this.value.set(Objects.requireNonNull(initialValue));
        this.increment = Objects.requireNonNull(increment);
    }

    @Override
    public BigInteger next() {
        return value.accumulateAndGet(increment, BigInteger::add);
        //equivalent of previous line
//        while (true) {
//            BigInteger current = value.get();
//            BigInteger next = current.add(increment);
//            if (value.compareAndSet(current, next)) {
//                return next;
//            }
//        }
    }

    @Override
    public BigInteger curval() {
        return value.get();
    }
}
