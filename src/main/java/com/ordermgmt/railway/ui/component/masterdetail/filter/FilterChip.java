package com.ordermgmt.railway.ui.component.masterdetail.filter;

/**
 * An active-filter chip: a human-readable label and the action that removes just this one
 * constraint (e.g. clearing a single date bound while leaving the rest of the filter intact).
 */
public record FilterChip(String label, Runnable clear) {}
