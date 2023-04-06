package de.blitzdose.infinitrack.components.notification;

import com.vaadin.flow.component.notification.NotificationVariant;

public class ErrorNotification extends AbstractNotification<ErrorNotification> {

    public ErrorNotification() {
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
