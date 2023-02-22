package de.blitzdose.infinitrack.views.station;

import com.fazecast.jSerialComm.SerialPort;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.blitzdose.infinitrack.serial.SerialCommunication;
import de.blitzdose.infinitrack.views.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@PageTitle("Base station")
@Route(value = "base-station", layout = MainLayout.class)
@CssImport(value = "./themes/infinitrack/components/textarea-console.css", themeFor="vaadin-text-area")
public class BaseStationView extends Div {

    String console = "";

    boolean openConnection = false;

    TextArea textArea = new TextArea("Console");
    Span statusSpan = new Span();
    Button connectButton = new Button("Connect");

    public BaseStationView(@Autowired SerialCommunication communication) {
        addClassName("base-station-view");

        UI ui = UI.getCurrent();
        ui.setPollInterval(1000);
        ui.addPollListener(new ComponentEventListener<PollEvent>() {
            @Override
            public void onComponentEvent(PollEvent event) {
                textArea.setValue(console);
                statusSpan.setText(openConnection ? "Connected" : "Disconnected");
                statusSpan.getElement().setAttribute("theme", "badge " + (openConnection ? "success" : "error"));
                connectButton.setText(openConnection ? "Disconnect" : "Connect");
            }
        });

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setHeightFull();

        HorizontalLayout horizontalLayout = new HorizontalLayout();

        Select<SerialPort> select = new Select<>();
        List<SerialPort> ports = Arrays.stream(SerialPort.getCommPorts()).toList();
        select.setItemLabelGenerator(SerialPort::getDescriptivePortName);
        select.setItems(ports);
        select.setPlaceholder("Select Port");
        calculateSelectWidth(select, ports);
        horizontalLayout.add(select);

        connectButton = new Button("Connect");
        connectButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        connectButton.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {
            @Override
            public void onComponentEvent(ClickEvent<Button> event) {
                SerialPort serialPort = select.getValue();
                if (openConnection) {
                    communication.closeConnection();
                } else {
                    if (serialPort == null) {
                        return;
                    }
                    connectToPort(serialPort, communication);
                }
            }
        });
        horizontalLayout.add(connectButton);

        statusSpan.setText("Disconnected");
        statusSpan.getElement().setAttribute("theme", "badge error");
        horizontalLayout.add(statusSpan);

        verticalLayout.add(horizontalLayout);

        textArea.setWidthFull();
        textArea.setHeightFull();
        textArea.setReadOnly(true);

        if (communication.isInitialized()) {
            Optional<SerialPort> serialPortOptional = ports.stream()
                    .filter(serialPort -> serialPort.getPortLocation().equals(communication.getSerialPort().getPortLocation()))
                    .findFirst();
            serialPortOptional.ifPresent(select::setValue);

            connectToPort(communication.getSerialPort(), communication);
        }

        communication.addOnDisconnectListener(new SerialCommunication.DisconnectListener() {
            @Override
            public void disconnect() {
                openConnection = false;
            }
        });

        verticalLayout.add(textArea);

        add(verticalLayout);
        setHeightFull();
    }

    private void connectToPort(SerialPort serialPort, SerialCommunication communication) {
        if (!communication.isInitialized()) {
            communication.initialize(serialPort, 115200);
        }
        if (!communication.isOpen()) {
            communication.openConnection();
        }
        if (communication.isOpen()) {
            openConnection = true;
        }

        console = communication.getConsole();
        textArea.setValue(console);
        communication.setOnDataListener(new SerialCommunication.DataListener() {
            @Override
            public void dataReceived(String msg, String console) {
                BaseStationView.this.console = console;
            }
        });
    }

    private void calculateSelectWidth(Select<SerialPort> select, List<SerialPort> labels) {
        labels.stream()
                .mapToInt(value -> value.getDescriptivePortName().length())
                .max()
                .ifPresent(width ->
                        select.setWidth(width + 5, Unit.CH)
                );
    }
}