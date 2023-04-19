package de.blitzdose.infinitrack.serial;

import com.vaadin.flow.spring.annotation.SpringComponent;
import org.springframework.web.context.annotation.ApplicationScope;

@ApplicationScope
@SpringComponent
public class SerialCommunicationListener {
    @ApplicationScope
    @SpringComponent
    public interface DataListener {
        void dataReceived(Message msg, String console);
    }

    @ApplicationScope
    @SpringComponent
    public interface DisconnectListener {
        void disconnect();
    }

    @ApplicationScope
    @SpringComponent
    public interface ConnectListener {
        void connect();
    }
}
