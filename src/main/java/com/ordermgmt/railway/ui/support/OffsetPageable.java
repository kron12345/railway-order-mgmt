package com.ordermgmt.railway.ui.support;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * A {@link Pageable} that honors an arbitrary (possibly non-page-aligned) offset — Spring Data JPA
 * applies {@code getOffset()}/{@code getPageSize()} directly as SQL OFFSET/LIMIT. Vaadin grids
 * fetch by offset+limit (not always a multiple of the page size), so the usual {@code
 * PageRequest.of(offset / limit, limit)} would return the wrong rows on a non-aligned offset; this
 * keeps the exact offset.
 */
public final class OffsetPageable implements Pageable {

    private final long offset;
    private final int limit;
    private final Sort sort;

    public OffsetPageable(long offset, int limit) {
        this(offset, limit, Sort.unsorted());
    }

    public OffsetPageable(long offset, int limit, Sort sort) {
        this.offset = Math.max(0, offset);
        this.limit = Math.max(1, limit);
        this.sort = sort == null ? Sort.unsorted() : sort;
    }

    @Override
    public int getPageNumber() {
        return (int) (offset / limit);
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetPageable(offset + limit, limit, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? new OffsetPageable(offset - limit, limit, sort) : first();
    }

    @Override
    public Pageable first() {
        return new OffsetPageable(0, limit, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetPageable((long) pageNumber * limit, limit, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}
