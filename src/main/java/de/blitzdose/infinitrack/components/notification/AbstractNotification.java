package de.blitzdose.infinitrack.components.notification;

import com.vaadin.flow.component.notification.Notification;

public abstract class AbstractNotification <T extends AbstractNotification <T>> {

    Notification notification = new Notification();

    public T setText(String text) {
        this.notification.setText(text);
        return (T) this;
    }

    public T setDuration(int duration) {
        this.notification.setDuration(duration);
        return (T) this;
    }

    public void open() {
        notification.open();
    }
}
