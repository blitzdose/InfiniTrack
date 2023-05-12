package de.blitzdose.infinitrack.views.devices;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.gridpro.GridPro;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.data.selection.SelectionListener;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoIcon;
import de.blitzdose.infinitrack.components.notification.ErrorNotification;
import de.blitzdose.infinitrack.components.notification.SuccessNotification;
import de.blitzdose.infinitrack.data.entities.BleDevice;
import de.blitzdose.infinitrack.data.entities.device.Device;
import de.blitzdose.infinitrack.data.services.DeviceService;
import de.blitzdose.infinitrack.serial.Message;
import de.blitzdose.infinitrack.serial.SerialCommunication;
import de.blitzdose.infinitrack.views.MainLayout;
import de.blitzdose.infinitrack.views.devices.dialogs.AddDeviceDialog;
import de.blitzdose.infinitrack.views.devices.dialogs.DeviceDialog;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@PageTitle("Devices")
@Route(value = "devices", layout = MainLayout.class)
@NpmPackage(value = "@datadobi/color-picker", version = "2.0.0-beta.4-datadobi8")
@JsModule("@datadobi/color-picker/color-picker.js")
public class DevicesView extends Div {

    private final DeviceService deviceService;
    private final SerialCommunication communication;
    private final GridPro<Device> grid = new GridPro<>();
    private GridListDataView<Device> gridListDataView;

    List<Device> devices;

    private final Button reloadButton = new Button(LumoIcon.RELOAD.create());
    private final TextField searchField = new TextField();
    private final Button addDeviceButton = new Button("Add Device", new Icon(VaadinIcon.PLUS));

    public DevicesView(@Autowired SerialCommunication communication, DeviceService deviceService) {
        this.deviceService = deviceService;
        this.communication = communication;
        addClassName("devices-view");
        setSizeFull();

        devices = getDevices();

        createView();
    }

    private void createView() {
        createGrid();

        createReloadButton();
        createSearchField();
        createAddDeviceButton();

        createLayout();
    }

    private void createLayout() {
        HorizontalLayout horizontalLayout = new HorizontalLayout(reloadButton, searchField, addDeviceButton);
        horizontalLayout.setWidth("100%");

        VerticalLayout layout = new VerticalLayout(horizontalLayout, grid);
        layout.setHeightFull();

        add(layout);
    }

    private void createAddDeviceButton() {
        addDeviceButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addDeviceButton.setMinWidth("auto");
        addDeviceButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) event -> {
            if (!communication.isOpen()) {
                new ErrorNotification().setText("Not connected to base station").open();
                return;
            }
            addDevice();
        });
    }

    private void addDevice() {
        AddDeviceDialog addDeviceDialog = new AddDeviceDialog()
                .setCloseListener(() -> communication.sendMessage(Message.MSG_STOP_SCAN))
                .setResultSelectedListener((dialog, bleDevice) -> {
                    dialog.close();
                    handleBleDeviceResult(bleDevice);
                })

                .setRepeatScanListener(() -> communication.sendMessage(Message.MSG_START_SCAN));
        addDeviceDialog.show();

        communication.setOnDataListener((msg, console) -> {
            if (msg.type().equals(Message.TYPE_SCAN_RESULT)) {
                BleDevice bleDevice = parseBleDevice(msg);
                addDeviceDialog.updateBleDevices(bleDevice);

            } else if (msg.type().equals(Message.TYPE_STATUS)) {
                if (msg.msg().equals(Message.MSG_SCAN_STOPPED)) {
                    DevicesView.this.getUI().ifPresent(ui -> ui.access(addDeviceDialog::setScanStopped));

                } else if (msg.msg().equals(Message.MSG_SCAN_STARTED)) {
                    DevicesView.this.getUI().ifPresent(ui -> ui.access(addDeviceDialog::setScanRunning));
                }
            }
        });

        communication.sendMessage(Message.MSG_START_SCAN);
    }

    private void handleBleDeviceResult(BleDevice bleDevice) {
        Dialog waitDialog = createWaitDialog();

        communication.sendMessage(Message.MSG_STOP_SCAN);
        communication.setOnDataListener(null);

        communication.setOnDataListener((msg, console) -> {
            if (msg.type().equals(Message.TYPE_STATUS)) {
                if (msg.msg().equals(Message.MSG_BLE_DATA_SEND)) {
                    showSuccessCreatingDevice(bleDevice, waitDialog);
                } else if (msg.msg().equals(Message.MSG_UNKNOWN_ERROR)) {
                    showErrorCreatingDevice(waitDialog);
                }
            }
        });
        communication.sendMessage(String.format(Message.MSG_BLE_CONNECT, bleDevice.getAddress()));
    }

    private void showSuccessCreatingDevice(BleDevice bleDevice, Dialog waitDialog) {
        DevicesView.this.getUI().ifPresent(ui -> ui.access(() -> {
            waitDialog.close();
            communication.setOnDataListener(null);

            saveDevice(createDevice(bleDevice.getAddressFormatted()));

            new SuccessNotification().setText("Device added").open();
        }));
    }

    private void showErrorCreatingDevice(Dialog waitDialog) {
        DevicesView.this.getUI().ifPresent(ui -> ui.access(() -> {
            waitDialog.close();
            communication.setOnDataListener(null);

            new ErrorNotification().setText("Error while communicating. Please try again.").open();
        }));
    }

    private Dialog createWaitDialog() {
        Dialog waitDialog = new Dialog();
        ProgressBar connectingProgressBar = new ProgressBar();
        connectingProgressBar.setIndeterminate(true);
        waitDialog.setHeaderTitle("Please wait...");
        waitDialog.add(connectingProgressBar);
        waitDialog.setCloseOnOutsideClick(false);
        waitDialog.setCloseOnEsc(false);
        waitDialog.open();
        return waitDialog;
    }

    private BleDevice parseBleDevice(Message msg) {
        JSONObject data = new JSONObject(msg.msg());
        String address = data.getString("addr");
        int rssi = data.getInt("rssi");
        JSONObject payload = data.getJSONObject("payload");

        BleDevice bleDevice = new BleDevice(address, rssi);
        bleDevice.setPayload(payload);
        if (payload.keySet().contains("9")) {
            bleDevice.setNameHEX(payload.getString("9"));
        }
        return bleDevice;
    }

    private void addFilterToGrid() {
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

    private void createReloadButton() {
        reloadButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        reloadButton.getElement().setAttribute("aria-label", "Reload");
        reloadButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) event -> refreshGrid());
    }

    private void createSearchField() {
        searchField.setWidth("100%");
        searchField.setPlaceholder("Search");
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> gridListDataView.refreshAll());
    }

    private void createGrid() {
        createGridComponent();
        addColumnsToGrid();
        addFilterToGrid();
    }

    private void createGridComponent() {
        grid.setSelectionMode(SelectionMode.SINGLE);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COLUMN_BORDERS);
        grid.setWidthFull();

        gridListDataView = grid.setItems(devices);

        grid.addSelectionListener((SelectionListener<Grid<Device>, Device>) selectionEvent -> {
            if (selectionEvent.getFirstSelectedItem().isPresent()) {
                Device device = selectionEvent.getFirstSelectedItem().get();
                handleDeviceClick(device);
                selectionEvent.getSource().deselectAll();
            }
        });
    }

    private void handleDeviceClick(Device clickedDevice) {
        new DeviceDialog(clickedDevice).setSaveListener((deviceDialog, device) -> {
            saveDevice(device);
            deviceDialog.close();
            new SuccessNotification().setText("Device saved").open();
        }).setDeleteListener((deviceDialog, device) -> {
            deleteDevice(device);
            deviceDialog.close();
        }).show();
    }

    private void saveDevice(Device device) {
        Optional<Device> optionalDevice = deviceService.get(device.getId());
        if (optionalDevice.isPresent()) {
            Device newDevice = optionalDevice.get();
            newDevice.setName(device.getName());
            newDevice.setColor(device.getColor());
            newDevice.setSapUUID(device.getSapUUID());
            device = newDevice;
        }
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
        createAddressColumn();
        createColorColumn();
        createNameColumn();
        createSignalColumn();
        createChargeLevelColumn();
        createStatusGPSColumn();
        createStatusColumn();
    }

    private void createIdColumn() {
        grid.addColumn(new NumberRenderer<>(Device::getId, NumberFormat.getNumberInstance(Locale.GERMANY)))
                .setComparator(Device::getId).setHeader("ID")
                .setFlexGrow(0);
    }

    private void createAddressColumn() {
        grid.addColumn(new TextRenderer<>(Device::getAddress))
                .setComparator(Device::getAddress).setHeader("Address");
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

    private void createChargeLevelColumn() {
        grid.addColumn(new NumberRenderer<>(Device::getChargeLevel, "%d %%"))
                .setHeader("Battery")
                .setFlexGrow(0);
    }

    private void createStatusGPSColumn() {
        grid.addColumn(new ComponentRenderer<>(device -> {
            Span span = new Span();
            span.setText(device.getStatusGPS());
            span.getElement().setAttribute("theme", "badge " + (device.getStatusGPS().equalsIgnoreCase("connected") ? "success" : "error"));
            return span;
        })).setComparator(Device::getStatusGPS).setHeader("GPS Status");
    }

    private void createStatusColumn() {
        grid.addColumn(new ComponentRenderer<>(device -> {
            Span span = new Span();
            span.setText(device.getStatus());
            span.getElement().setAttribute("theme", "badge " + (device.getStatus().equalsIgnoreCase("connected") ? "success" : "error"));
            return span;
        })).setComparator(Device::getStatus).setHeader("Status");
    }

    private List<Device> getDevices() {
        return deviceService.list();
    }

    private Device createDevice(String address) {
        Device c = new Device();
        c.setName("New Device");
        c.setAddress(address);
        c.setColor("#000000");
        c.setStatus("Offline");
        c.setStatusGPS("Offline");

        deviceService.update(c);

        return c;
    }

    private boolean matchesTerm(String value, String searchTerm) {
        return value.toLowerCase().contains(searchTerm.toLowerCase());
    }

}
