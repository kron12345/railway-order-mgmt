package com.ordermgmt.railway.ui.view.login;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Login view that redirects to Keycloak OIDC login. Automatically redirects unauthenticated users
 * to the OAuth2 authorization endpoint.
 */
@Route("login")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().set("background", "var(--rom-bg-primary)");

        Div card = new Div();
        card.getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "8px")
                .set("padding", "var(--lumo-space-xl)")
                .set("text-align", "center")
                .set("max-width", "400px");

        H2 title = new H2(getTranslation("app.title"));
        title.getStyle()
                .set("color", "var(--rom-accent)")
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("margin-bottom", "var(--lumo-space-m)");

        Anchor loginLink =
                new Anchor("/oauth2/authorization/keycloak", getTranslation("login.button"));
        loginLink
                .getStyle()
                .set("display", "inline-block")
                .set("background", "var(--rom-accent)")
                .set("color", "var(--rom-bg-primary)")
                .set("padding", "var(--lumo-space-s) var(--lumo-space-l)")
                .set("border-radius", "6px")
                .set("font-weight", "600")
                .set("text-decoration", "none")
                .set("font-size", "var(--lumo-font-size-m)");
        loginLink.setRouterIgnore(true);

        card.add(title, loginLink);
        add(card);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            Paragraph error = new Paragraph(getTranslation("login.error"));
            error.getStyle().set("color", "var(--rom-status-danger)");
            add(error);
        }
    }
}
