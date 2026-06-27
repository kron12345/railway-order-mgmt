package com.ordermgmt.railway.ui.component.masterdetail;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Accumulating page loader for a lazy {@link MasterDetailLayout}. Holds the items loaded so far and
 * whether a further page exists; the layout owns rendering and calls {@code onChange} after each
 * append to re-render + refresh the readout.
 *
 * <p>Loads are synchronous (a DB call on the Vaadin UI thread, like the legacy {@code setItems}
 * path). Vaadin processes each client round-trip atomically and in order, so a filter change and an
 * auto-load callback never interleave mid-load — {@link #loadNext} always reads the
 * <em>current</em> filter text and the <em>current</em> offset, so an out-of-order page can't be
 * appended. That is why no generation/stale token is needed here.
 *
 * <p>Offsets are always exact multiples of the loader's page size because only whole pages are ever
 * appended (the final short page just ends the list); this keeps {@code OffsetPageable} + {@code
 * PageRequest.of(offset / size, size)} reconstruction correct. A future "jump to row N" feature
 * would break that invariant. The page size lives in the loader closure (its {@code
 * OffsetPageable}), so this controller never needs to know it.
 */
class LazyListController<T> {

    private final BiFunction<String, Integer, SliceResult<T>> loader;
    private final Runnable onChange;

    private final List<T> loaded = new ArrayList<>();
    private boolean hasMore = true;
    private boolean loading = false;
    private boolean started = false;

    LazyListController(BiFunction<String, Integer, SliceResult<T>> loader, Runnable onChange) {
        this.loader = loader;
        this.onChange = onChange;
    }

    List<T> items() {
        return loaded;
    }

    int loadedCount() {
        return loaded.size();
    }

    boolean hasMore() {
        return hasMore;
    }

    /** Whether the first page has ever been loaded (drives {@code ensureLoaded}). */
    boolean isStarted() {
        return started;
    }

    /** Clear and load the first page for the given filter text. */
    void reset(String filterText) {
        loaded.clear();
        hasMore = true;
        loading = false;
        started = true;
        loadNext(filterText);
    }

    /** Append the next page (no-op while loading or when exhausted). */
    void loadNext(String filterText) {
        if (loading || !hasMore) {
            return;
        }
        loading = true;
        try {
            SliceResult<T> nextPage = loader.apply(filterText, loaded.size());
            if (nextPage != null) {
                loaded.addAll(nextPage.items());
                hasMore = nextPage.hasNext();
            } else {
                hasMore = false;
            }
        } finally {
            loading = false;
        }
        onChange.run();
    }
}
