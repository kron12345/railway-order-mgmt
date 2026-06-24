package com.ordermgmt.railway.ui.component.business;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class AssigneeComboBoxTest {

    /**
     * Regression: {@code hashCode()} used {@code Objects.hashCode(this)}, which calls {@code
     * this.hashCode()} and recurses into a {@link StackOverflowError}. The order overview puts
     * these combos into hash-based structures, so navigating to Aufträge crashed the view.
     */
    @Test
    void hashCodeIsIdentityBasedAndDoesNotRecurse() {
        // keycloakUserService is only dereferenced by the lazy dropdown query, not by
        // construction / hashCode / equals — so null is safe here and keeps the test
        // dependency-free.
        AssigneeComboBox combo = new AssigneeComboBox(null, (type, value) -> {});

        assertThat(combo.hashCode()).isEqualTo(System.identityHashCode(combo));

        AssigneeComboBox other = new AssigneeComboBox(null, (type, value) -> {});
        assertThat(combo).isEqualTo(combo).isNotEqualTo(other);

        // Mirrors how the overview holds the combos — must not blow up.
        Set<AssigneeComboBox> set = new HashSet<>();
        set.add(combo);
        set.add(other);
        assertThat(set).containsExactlyInAnyOrder(combo, other);
    }
}
