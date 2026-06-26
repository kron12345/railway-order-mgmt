package com.ordermgmt.railway.ui.component.masterdetail;

import java.util.List;

/**
 * One page of a lazy master/detail list: the items just fetched plus whether a further page exists.
 * Framework-agnostic on purpose — the view adapts Spring's {@code Slice} (or any source) to this so
 * {@link MasterDetailLayout} never depends on Spring Data. Matches the "no total count" philosophy
 * of the lazy list (we never know the full size, only whether there is more).
 */
public record SliceResult<T>(List<T> items, boolean hasNext) {}
