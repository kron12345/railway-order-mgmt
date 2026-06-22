/**
 * <strong>User View Preferences</strong> — generic per-user persistence for arbitrary view state.
 *
 * <p>One table {@code user_view_preferences} (V30 migration) maps {@code (user_id, view_key) → JSON
 * payload}. Currently used by {@link com.ordermgmt.railway.ui.component.grid.GridPreferenceBinder}
 * for grid column order / width / visibility, but designed generically so future state (splitter
 * positions, filter selections, dashboard layouts) can persist into the same table by picking a
 * fresh {@code view_key}.
 *
 * <p>The user identity comes from the OAuth principal (preferred_username from Keycloak, falling
 * back to {@code Authentication.getName()}).
 */
package com.ordermgmt.railway.domain.userprefs;
