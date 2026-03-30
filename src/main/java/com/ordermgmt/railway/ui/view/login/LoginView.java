package com.ordermgmt.railway.ui.view.login;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Login view that redirects to Keycloak OIDC login.
 * In production, Spring Security automatically redirects to Keycloak.
 * This view is shown only when explicitly navigated to or on auth errors.
 */
@Route("login")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H2 title = new H2(getTranslation("login.title"));
        Paragraph info = new Paragraph(getTranslation("login.redirect.info"));

        add(title, info);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            add(new Paragraph(getTranslation("login.error")));
        }
    }
}
