package com.ordermgmt.railway.ui.component.masterdetail;

/** Three-argument functional interface used for the master-detail announce template. */
@FunctionalInterface
public interface TriFunction<A, B, C, R> {
    R apply(A a, B b, C c);
}
