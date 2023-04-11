package de.blitzdose.infinitrack.views.devices;

import com.github.juchar.colorpicker.ColorPickerRaw;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.gridpro.GridPro;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.map.configuration.Coordinate;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.data.selection.SelectionEvent;
import com.vaadin.flow.data.selection.SelectionListener;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.blitzdose.infinitrack.components.notification.ErrorNotification;
import de.blitzdose.infinitrack.components.notification.SuccessNotification;
import de.blitzdose.infinitrack.data.entities.BleDevice;
import de.blitzdose.infinitrack.data.entities.device.Device;
import de.blitzdose.infinitrack.data.services.DeviceService;
import de.blitzdose.infinitrack.serial.SerialCommunication;
import de.blitzdose.infinitrack.views.MainLayout;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.olli.ClipboardHelper;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@PageTitle("Devices")
@Route(value = "devices", layout = MainLayout.class)
@NpmPackage(value = "@datadobi/color-picker", version = "2.0.0-beta.4-datadobi8")
@JsModule("@datadobi/color-picker/color-picker.js")
public class DevicesView extends Div {

    SerialCommunication communication;
    private GridPro<Device> grid;
    GridListDataView<Device> gridListDataView;

    DeviceService deviceService;

    List<Device> devices;

    public DevicesView(@Autowired SerialCommunication communication, DeviceService deviceService) {
        this.deviceService = deviceService;
        this.communication = communication;
        addClassName("devices-view");
        setSizeFull();
        grid = createGrid();

        TextField searchField = createSearchField();
        addFilterToGrid(searchField);

        Button addDeviceButton = createAddDeviceButton();

        createLayout(searchField, addDeviceButton);
    }

    private void createLayout(TextField searchField, Button addDeviceButton) {
        HorizontalLayout horizontalLayout = new HorizontalLayout(searchField, addDeviceButton);
        horizontalLayout.setWidth("100%");

        VerticalLayout layout = new VerticalLayout(horizontalLayout, grid);
        layout.setHeightFull();

        add(layout);
    }

    private Button createAddDeviceButton() {
        Button addDeviceBtn = new Button("Add Device", new Icon(VaadinIcon.PLUS));
        addDeviceBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addDeviceBtn.setMinWidth("auto");
        addDeviceBtn.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {
            @Override
            public void onComponentEvent(ClickEvent<Button> event) {
                if (!communication.isOpen()) {
                    new ErrorNotification().setText("Not connected to base station").open();
                    return;
                }

                Dialog addDeviceDialog = new Dialog();

                addDeviceDialog.setHeaderTitle("Add device");
                addDeviceDialog.setCloseOnEsc(false);
                addDeviceDialog.setCloseOnOutsideClick(false);

                Button closeButton = new Button(new Icon("lumo", "cross"),
                        (e) -> {
                            addDeviceDialog.close();
                            communication.sendMessage(SerialCommunication.MSG_STOP_SCAN);

                        });
                closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                addDeviceDialog.getHeader().add(closeButton);

                VerticalLayout dialogLayout = new VerticalLayout();

                ProgressBar progressBar = new ProgressBar();
                progressBar.setIndeterminate(true);
                progressBar.setVisible(false);

                Div progressBarLabel = new Div();
                progressBarLabel.setText("Scanning for devices...");
                progressBarLabel.setVisible(false);

                dialogLayout.add(progressBarLabel, progressBar);

                ArrayList<BleDevice> devices = new ArrayList<>();

                Grid<BleDevice> scanResults = new Grid<>(BleDevice.class, false);
                scanResults.addColumn(BleDevice::getName).setHeader("Name").setAutoWidth(true);
                scanResults.addColumn(BleDevice::getAddressFormatted).setHeader("Address").setAutoWidth(true);
                scanResults.addColumn(BleDevice::getRssi).setHeader("Signal (RSSI)").setAutoWidth(true).setFlexGrow(0);

                scanResults.setItems(devices);

                scanResults.addItemClickListener(new ComponentEventListener<ItemClickEvent<BleDevice>>() {
                    @Override
                    public void onComponentEvent(ItemClickEvent<BleDevice> event) {
                        BleDevice bleDevice = event.getItem();
                        communication.sendMessage(SerialCommunication.MSG_STOP_SCAN);
                        communication.setOnDataListener(null);
                        communication.sendMessage(String.format(SerialCommunication.MSG_BLE_CONNECT, bleDevice.getAddress()));
                        addDeviceDialog.close();

                        Dialog waitDialog = new Dialog();
                        ProgressBar connectingProgressBar = new ProgressBar();
                        connectingProgressBar.setIndeterminate(true);
                        waitDialog.setHeaderTitle("Please wait...");
                        waitDialog.add(connectingProgressBar);
                        waitDialog.setCloseOnOutsideClick(false);
                        waitDialog.setCloseOnEsc(false);
                        waitDialog.open();

                        communication.setOnDataListener(new SerialCommunication.DataListener() {
                            @Override
                            public void dataReceived(JSONObject msg, String console) {
                                if (msg.getString("type").equals(SerialCommunication.TYPE_STATUS) &&
                                        msg.getString("msg").equals(SerialCommunication.MSG_BLE_DATA_SEND)) {
                                    DevicesView.this.getUI().ifPresent(ui -> {
                                        ui.access(() -> {
                                            waitDialog.close();
                                            communication.setOnDataListener(null);
                                            new SuccessNotification().setText("Device added").open();
                                        });
                                    });
                                }
                            }
                        });
                    }
                });

                dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
                dialogLayout.getStyle().set("min-width", "38rem").set("max-width", "100%");
                dialogLayout.setSpacing(false);
                dialogLayout.setPadding(false);

                addDeviceDialog.add(dialogLayout, scanResults);

                Button repeatScanButton = new Button("Repeat scan", click -> {
                    devices.clear();
                    scanResults.setItems(devices);
                    communication.sendMessage(SerialCommunication.MSG_START_SCAN);
                });
                repeatScanButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                repeatScanButton.getStyle().set("margin-right", "auto");
                repeatScanButton.setEnabled(false);
                addDeviceDialog.getFooter().add(repeatScanButton);

                addDeviceDialog.open();

                UI.getCurrent().addPollListener(new ComponentEventListener<PollEvent>() {
                    @Override
                    public void onComponentEvent(PollEvent pollEvent) {
                        List<BleDevice> filteredBleDevices = devices.stream().filter(bleDevice -> bleDevice.getPayload().keySet().contains("255") &&
                                bleDevice.getPayload().get("255").equals(SerialCommunication.BLE_SIGNATURE)).toList();
                        scanResults.setItems(filteredBleDevices);
                    }
                });
                UI.getCurrent().setPollInterval(1000);

                communication.setOnDataListener(new SerialCommunication.DataListener() {
                    @Override
                    public void dataReceived(JSONObject msg, String console) {
                        if (msg.getString("type").equals(SerialCommunication.TYPE_SCAN_RESULT)) {
                            JSONObject data = msg.getJSONObject("msg");
                            String address = data.getString("addr");
                            int rssi = data.getInt("rssi");
                            JSONObject payload = data.getJSONObject("payload");

                            BleDevice bleDevice = new BleDevice(address, rssi);
                            bleDevice.setPayload(payload);
                            if (payload.keySet().contains("9")) {
                                bleDevice.setNameHEX(payload.getString("9"));
                            }

                            devices.stream()
                                    .filter(bleDevice1 -> bleDevice1.equals(bleDevice))
                                    .forEach(bleDevice::mergeFrom);

                            devices.remove(bleDevice);
                            devices.add(bleDevice);
                        } else if (msg.getString("type").equals(SerialCommunication.TYPE_STATUS)) {
                            if (msg.getString("msg").equals(SerialCommunication.MSG_SCAN_STOPPED)) {
                                DevicesView.this.getUI().ifPresent(ui -> {
                                    ui.access(() -> {
                                        progressBar.setVisible(false);
                                        progressBarLabel.setVisible(false);
                                        repeatScanButton.setEnabled(true);
                                    });
                                });
                            } else if (msg.getString("msg").equals(SerialCommunication.MSG_SCAN_STARTED)) {
                                DevicesView.this.getUI().ifPresent(ui -> {
                                    ui.access(() -> {
                                        progressBar.setVisible(true);
                                        progressBarLabel.setVisible(true);
                                        repeatScanButton.setEnabled(false);
                                    });
                                });
                            }
                        }
                    }
                });

                communication.sendMessage(SerialCommunication.MSG_START_SCAN);
            }
        });
        return addDeviceBtn;
    }

    private void addFilterToGrid(TextField searchField) {
        gridListDataView.addFilter(device -> {
            String searchTerm = searchField.getValue().trim();

            if (searchTerm.isEmpty())
                return true;

            boolean matchesName = matchesTerm(device.getName(),
                    searchTerm);
            boolean matchesId = matchesTerm(String.valueOf(device.getId()), searchTerm);
            boolean matchesStatus = matchesTerm(device.getStatus(),
                    searchTerm);

            return matchesName || matchesId || matchesStatus;
        });
    }

    private TextField createSearchField() {
        TextField searchField = new TextField();
        searchField.setWidth("100%");
        searchField.setPlaceholder("Search");
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> gridListDataView.refreshAll());
        return searchField;
    }

    private GridPro<Device> createGrid() {
        GridPro<Device> grid = createGridComponent();
        addColumnsToGrid();
        return grid;
    }

    private GridPro<Device> createGridComponent() {
        grid = new GridPro<>();
        grid.setSelectionMode(SelectionMode.SINGLE);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COLUMN_BORDERS);
        grid.setWidthFull();

        devices = getDevices();
        gridListDataView = grid.setItems(devices);

        grid.addSelectionListener(new SelectionListener<Grid<Device>, Device>() {
            @Override
            public void selectionChange(SelectionEvent<Grid<Device>, Device> selectionEvent) {
                if (selectionEvent.getFirstSelectedItem().isPresent()) {
                    Device device = selectionEvent.getFirstSelectedItem().get();
                    selectionEvent.getSource().deselectAll();
                    showDeviceDialog(device);
                }
            }
        });
        return grid;
    }

    private void showDeviceDialog(Device device) {
        Dialog deviceDialog = new Dialog();
        Span span = new Span();
        span.setText(device.getStatus());
        span.getElement().setAttribute("theme", "badge " + (device.getStatus().equalsIgnoreCase("connected") ? "success" : "error"));
        deviceDialog.setHeaderTitle(String.format("Device %d", device.getId()));

        VerticalLayout dialogLayout = new VerticalLayout();

        dialogLayout.add(span);

        TextField nameTextField = new TextField("Name");
        nameTextField.setValue(device.getName());
        dialogLayout.add(nameTextField);;

        HorizontalLayout colorLayout = new HorizontalLayout();

        Icon icon = new Icon(VaadinIcon.CIRCLE);
        icon.setColor(device.getColor());
        icon.getStyle().set("align-self", "end");
        icon.getStyle().set("margin", "var(--lumo-space-xs) 0");
        icon.getStyle().set("height", "36px");
        icon.getStyle().set("width", "36px");

        TextField colorTextField = new TextField("Color");
        colorTextField.setValue(device.getColor());
        colorTextField.getStyle().set("flex-grow", "1");
        colorLayout.add(colorTextField);
        colorTextField.addFocusListener(new ComponentEventListener<FocusNotifier.FocusEvent<TextField>>() {
            @Override
            public void onComponentEvent(FocusNotifier.FocusEvent<TextField> event) {
                Dialog dialog = new Dialog();

                ColorPickerRaw colorPicker = new ColorPickerRaw(event.getSource().getValue(), event.getSource().getValue());
                colorPicker.setHslEnabled(false);
                colorPicker.setRgbEnabled(false);
                colorPicker.setAlphaEnabled(false);

                dialog.add(colorPicker);

                Button saveButton = new Button("Save");
                saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                saveButton.addClickListener(clickEvent -> {
                   event.getSource().setValue(colorPicker.getValue());
                   dialog.close();
                });

                Button cancelButton = new Button("Cancel");
                cancelButton.addClickListener(clickEvent -> dialog.close());
                dialog.getFooter().add(cancelButton);
                dialog.getFooter().add(saveButton);
                dialog.open();
            }
        });
        colorTextField.addValueChangeListener(event -> icon.setColor(event.getValue()));

        colorLayout.add(icon);
        dialogLayout.add(colorLayout);

        TextField versionTextField = new TextField("Firmware version");
        versionTextField.setReadOnly(true);
        versionTextField.setValue(device.getFirmwareVersion());
        dialogLayout.add(versionTextField);

        TextField lastLocationTextField = new TextField("Last known location");
        lastLocationTextField.setReadOnly(true);
        lastLocationTextField.setValue(device.getCoordinate() != null ? String.format(Locale.ROOT, "%f, %f", device.getCoordinate().getY(), device.getCoordinate().getX()) : "Unknown");
        lastLocationTextField.setWidthFull();
        Span copyIcon = new Span();
        copyIcon.setClassName("la la-copy");
        Button button = new Button("Copy to clipboard", copyIcon);
        ClipboardHelper clipboardHelper = new ClipboardHelper(lastLocationTextField.getValue(), button);
        clipboardHelper.getElement().addEventListener("click", event -> {
            Notification.show("Copied to clipboard");
        });
        HorizontalLayout horizontalLayout = new HorizontalLayout(lastLocationTextField, clipboardHelper);
        horizontalLayout.setAlignItems(FlexComponent.Alignment.END);
        dialogLayout.add(horizontalLayout);
        //lastLocationTextField.getElement().addEventListener("click", clickEvent -> {
        //    ClientsideClipboard.writeToClipboard(lastLocationTextField.getValue(), successful ->
        //            Notification.show(successful ? "Copied to clipboard" : "Could not write to clipboard")
        //    );
//
        //});
        //dialogLayout.add(lastLocationTextField);

        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("min-width", "28rem").set("max-width", "100%");
        dialogLayout.setSpacing(false);
        dialogLayout.setPadding(false);

        deviceDialog.add(dialogLayout);


        Button closeButton = new Button(new Icon("lumo", "cross"),
                (e) -> deviceDialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        deviceDialog.getHeader().add(closeButton);


        Button saveButton = new Button("Save", e -> {
            device.setName(nameTextField.getValue());
            device.setColor(colorTextField.getValue());
            saveDevice(device);
            deviceDialog.close();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button deleteButton = new Button("Delete", click -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(String.format("Delete device %d?", device.getId()));
            confirmDialog.setText(
                    "Are you sure you want to delete the Device? This cannot be undone.");

            confirmDialog.setRejectable(true);
            confirmDialog.setRejectText("No");
            confirmDialog.addRejectListener(event -> confirmDialog.close());

            confirmDialog.setConfirmText("Yes");
            confirmDialog.addConfirmListener(event -> {
                deleteDevice(device);
                deviceDialog.close();
            });

            confirmDialog.open();
        });
        deleteButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        deleteButton.getStyle().set("margin-right", "auto");

        deviceDialog.getFooter().add(deleteButton);
        deviceDialog.getFooter().add(saveButton);

        deviceDialog.open();

        //final GPX gpx = GPX.builder()
        //        .creator("InfiniTrack")
        //        .addTrack(track -> track
        //                .addSegment(segment -> segment
        //                        .addPoint(p -> p.lat(48.20100).lon(16.31651).ele(283))
        //                        .addPoint(p -> p.lat(48.20112).lon(16.31639).ele(278))
        //                        .addPoint(p -> p.lat(48.20126).lon(16.31601).ele(274))))
        //        .build();
//
        //ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        //try {
        //    GPX.Writer.DEFAULT.write(gpx, outputStream);
        //    byte[] gpxBytes = outputStream.toByteArray();
        //    outputStream.flush();
        //    outputStream.close();
        //} catch (IOException e) {
        //    throw new RuntimeException(e);
        //}
    }

    private void saveDevice(Device device) {
        deviceService.update(device);
        refreshGrid();
    }

    private void deleteDevice(Device device) {
        deviceService.delete(device.getId());
        refreshGrid();
    }

    private void refreshGrid() {
        devices.clear();
        devices.addAll(deviceService.list());
        gridListDataView.refreshAll();
    }

    private void addColumnsToGrid() {
        createIdColumn();
        createColorColumn();
        createNameColumn();
        createSignalColumn();
        createStatusColumn();
    }

    private void createIdColumn() {
        grid.addColumn(new NumberRenderer<>(Device::getId, NumberFormat.getNumberInstance(Locale.GERMANY)))
                .setComparator(Device::getId).setHeader("ID")
                .setFlexGrow(0);
    }

    private void createColorColumn() {
        grid.addColumn(new ComponentRenderer<>(device -> {
                    Icon icon = new Icon(VaadinIcon.CIRCLE);
                    icon.setColor(device.getColor());
                    return icon;
                }))
                .setTextAlign(ColumnTextAlign.CENTER)
                .setHeader("Color")
                .setFlexGrow(0);
    }

    private void createNameColumn() {
        grid.addColumn(new TextRenderer<>(Device::getName))
                .setComparator(Device::getName).setHeader("Name");
    }

    private void createSignalColumn() {
        grid.addColumn(new NumberRenderer<>(Device::getSignal, "%.2f dB", Locale.GERMANY))
                .setComparator(Device::getSignal).setHeader("Signal");
    }

    private void createStatusColumn() {
        grid.addColumn(new ComponentRenderer<>(device -> {
            Span span = new Span();
            span.setText(device.getStatus());
            span.getElement().setAttribute("theme", "badge " + (device.getStatus().equalsIgnoreCase("connected") ? "success" : "error"));
            return span;
        })).setComparator(Device::getStatus).setHeader("Status");
    }

    private boolean areStatusesEqual(Device device, ComboBox<String> statusFilter) {
        String statusFilterValue = statusFilter.getValue();
        if (statusFilterValue != null) {
            return StringUtils.equals(device.getStatus(), statusFilterValue);
        }
        return true;
    }

    private List<Device> getDevices() {
        if (deviceService.count() == 0) {
            Arrays.asList(
                    createDevice(1, "Amarachi Nkechi", 18.58, "Connected", "1.0"),
                    createDevice(2, "Bonelwa Ngqawana", 18.42, "Connected", "1.1"),
                    createDevice(3, "Debashis Bhuiyan", 25.71, "Connected", "1.0"),
                    createDevice(4, "Jacqueline Asong", -50.70, "Offline", "1.0"),
                    createDevice(5, "Kobus van de Vegte", 37.99, "Connected", "1.0"),
                    createDevice(6, "Mattie Blooman", -23.74, "Offline", "1.0"),
                    createDevice(7, "Oea Romana", -49.55, "Offline", "1.0"),
                    createDevice(8, "Stephanus Huggins", 65.06, "Connected", "1.0"),
                    createDevice(9, "Torsten Paulsson", 28.77, "Offline", "1.0"));
        }
        return deviceService.list();
    }

    private Device createDevice(long id, String name, double amount, String status, String firmwareVersion) {
        Device c = new Device();
        c.setId(id);
        c.setColor("#000000");
        c.setCoordinate(new Coordinate(9.070210, 48.514922));
        c.setName(name);
        c.setSignal(amount);
        c.setStatus(status);
        c.setFirmwareVersion(firmwareVersion);

        deviceService.update(c);

        return c;
    }

    private boolean matchesTerm(String value, String searchTerm) {
        return value.toLowerCase().contains(searchTerm.toLowerCase());
    }

};
