package de.blitzdose.infinitrack.components.notification;

import com.vaadin.flow.component.notification.NotificationVariant;

public class SuccessNotification extends AbstractNotification<SuccessNotification> {
    public SuccessNotification() {
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}
