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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.blitzdose.infinitrack.serial.SerialCommunication;
import de.blitzdose.infinitrack.views.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@PageTitle("Base station")
@Route(value = "base-station", layout = MainLayout.class)
@CssImport(value = "./themes/infinitrack/components/textarea-console.css", themeFor="vaadin-text-area")
public class BaseStationView extends Div {

    private final SerialCommunication communication;
    private boolean openConnection = false;

    private String console = "";

    private final TextArea consoleTextArea = new TextArea("Console");
    private final Span statusSpan = new Span();
    private final Button connectButton = new Button("Connect");
    private final Select<SerialPort> select = new Select<>();

    public BaseStationView(@Autowired SerialCommunication communication) {
        addClassName("base-station-view");
        setHeightFull();

        this.communication = communication;

        registerPollListener();
        createView();
    }

    private void createView() {
        communication.addOnDisconnectListener(() -> openConnection = false);

        HorizontalLayout portSelectionLayout = createPortSelectionLayout();

        createTextArea();

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setHeightFull();
        verticalLayout.add(portSelectionLayout, consoleTextArea);
        add(verticalLayout);
    }

    private void createTextArea() {
        consoleTextArea.setWidthFull();
        consoleTextArea.setMinHeight("50%");
        consoleTextArea.getStyle().set("flex-grow", "1");
        consoleTextArea.setReadOnly(true);
    }

    private HorizontalLayout createPortSelectionLayout() {
        HorizontalLayout horizontalLayout = new HorizontalLayout();

        createPortSelect();
        createConnectButton();
        createStatusSpan();

        horizontalLayout.add(select, connectButton, statusSpan);
        return horizontalLayout;
    }

    private void createStatusSpan() {
        statusSpan.setText("Disconnected");
        statusSpan.getElement().setAttribute("theme", "badge error");
    }

    private void createConnectButton() {
        connectButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        connectButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) event -> {
            SerialPort serialPort = select.getValue();
            if (openConnection) {
                communication.closeConnection();
            } else {
                if (serialPort == null) {
                    return;
                }
                connectToPort(serialPort, communication);
            }
        });
    }

    private void createPortSelect() {
        List<SerialPort> ports = Arrays.stream(SerialPort.getCommPorts()).toList();
        select.setItemLabelGenerator(SerialPort::getDescriptivePortName);
        select.setItems(ports);
        select.setPlaceholder("Select Port");
        calculateSelectWidth(select, ports);

        if (communication.isInitialized()) {
            System.out.println("set port");
            Optional<SerialPort> serialPortOptional = ports.stream()
                    .filter(serialPort -> serialPort.
                            getPortLocation().
                            equals(communication.getSerialPort().getPortLocation()))
                    .findFirst();
            serialPortOptional.ifPresent(select::setValue);

            connectToPort(communication.getSerialPort(), communication);
        }
    }

    private void registerPollListener() {
        UI ui = UI.getCurrent();
        ui.setPollInterval(1000);
        ui.addPollListener((ComponentEventListener<PollEvent>) event -> {
            consoleTextArea.setValue(console);
            consoleTextArea.getElement()
                    .executeJs("this.shadowRoot.querySelector(\"vaadin-input-container\").scrollTop = this.querySelector(\"textarea\").clientHeight");
            statusSpan.setText(openConnection ? "Connected" : "Disconnected");
            statusSpan.getElement()
                    .setAttribute("theme", "badge " + (openConnection ? "success" : "error"));
            connectButton.setText(openConnection ? "Disconnect" : "Connect");
        });
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
        consoleTextArea.setValue(console);
        communication.setOnDataListener((msg, console) -> BaseStationView.this.console = console);
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
