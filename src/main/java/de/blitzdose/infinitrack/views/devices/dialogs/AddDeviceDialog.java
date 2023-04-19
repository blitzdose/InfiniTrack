package de.blitzdose.infinitrack.views.devices.dialogs;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.PollEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import de.blitzdose.infinitrack.data.entities.BleDevice;
import de.blitzdose.infinitrack.serial.Message;

import java.util.ArrayList;
import java.util.List;

public class AddDeviceDialog {

    private final Dialog dialog;
    private final ArrayList<BleDevice> bleDevices = new ArrayList<>();

    private CloseListener closeListener;
    private ResultSelectedListener resultSelectedListener;
    private RepeatScanListener repeatScanListener;

    private final Div progressBarLabel = new Div();
    private final ProgressBar progressBar = new ProgressBar();
    private final Button repeatScanButton = new Button("Repeat scan");

    private final Grid<BleDevice> scanResults = new Grid<>(BleDevice.class, false);

    public AddDeviceDialog() {
        this.dialog = new Dialog();

        initialize();
        registerPollListener();
    }

    private void initialize() {
        dialog.setHeaderTitle("Add device");
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);

        createCloseButton();
        createDialogLayout();
        createScanResultsComponent();
        createFooter();
    }

    private void createFooter() {
        repeatScanButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            bleDevices.clear();
            scanResults.setItems(bleDevices);
            if (repeatScanListener != null) {
                repeatScanListener.onRepeatScan();
            }
        });
        repeatScanButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        repeatScanButton.getStyle().set("margin-right", "auto");
        repeatScanButton.setEnabled(false);
        dialog.getFooter().add(repeatScanButton);
    }

    private void createScanResultsComponent() {
        addScanResultColumns(scanResults);
        scanResults.setItems(bleDevices);
        scanResults.addItemClickListener((ComponentEventListener<ItemClickEvent<BleDevice>>) event -> {
            BleDevice bleDevice = event.getItem();
            if (resultSelectedListener != null) {
                resultSelectedListener.onResultSelected(AddDeviceDialog.this, bleDevice);
            }
        });
        dialog.add(scanResults);
    }

    private void addScanResultColumns(Grid<BleDevice> scanResults) {
        scanResults.addColumn(BleDevice::getName).setHeader("Name").setAutoWidth(true);
        scanResults.addColumn(BleDevice::getAddressFormatted).setHeader("Address").setAutoWidth(true);
        scanResults.addColumn(BleDevice::getRssi).setHeader("Signal (RSSI)").setAutoWidth(true).setFlexGrow(0);
    }

    private void createDialogLayout() {
        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("min-width", "38rem").set("max-width", "100%");
        dialogLayout.setSpacing(false);
        dialogLayout.setPadding(false);

        createProgressBarLabel(dialogLayout);
        createProgressBar(dialogLayout);

        dialog.add(dialogLayout);
    }

    private void createProgressBar(VerticalLayout dialogLayout) {
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        dialogLayout.add(progressBar);
    }

    private void createProgressBarLabel(VerticalLayout dialogLayout) {
        progressBarLabel.setText("Scanning for devices...");
        progressBarLabel.setVisible(false);
        dialogLayout.add(progressBarLabel);
    }

    private void createCloseButton() {
        Button closeButton = new Button(new Icon("lumo", "cross"),
                (e) -> {
                    this.dialog.close();
                    if (closeListener != null) {
                        closeListener.onClose();
                    }
                });
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getHeader().add(closeButton);
    }

    public void show() {
        this.dialog.open();
    }

    public void close() {
        this.dialog.close();
    }

    public void updateBleDevices(BleDevice bleDevice) {
        bleDevices.stream()
                .filter(bleDevice1 -> bleDevice1.equals(bleDevice))
                .forEach(bleDevice::mergeFrom);

        bleDevices.remove(bleDevice);
        bleDevices.add(bleDevice);
    }

    private void registerPollListener() {
        UI.getCurrent().addPollListener((ComponentEventListener<PollEvent>) pollEvent -> {
            List<BleDevice> filteredBleDevices = bleDevices.stream().filter(bleDevice -> bleDevice.getPayload().keySet().contains("255") &&
                    bleDevice.getPayload().get("255").equals(Message.BLE_SIGNATURE)).toList();
            scanResults.setItems(filteredBleDevices);
        });
        UI.getCurrent().setPollInterval(1000);
    }

    public AddDeviceDialog setCloseListener(CloseListener closeListener) {
        this.closeListener = closeListener;
        return this;
    }

    public AddDeviceDialog setResultSelectedListener(ResultSelectedListener resultSelectedListener) {
        this.resultSelectedListener = resultSelectedListener;
        return this;
    }

    public AddDeviceDialog setRepeatScanListener(RepeatScanListener repeatScanListener) {
        this.repeatScanListener = repeatScanListener;
        return this;
    }

    public void setScanStopped() {
        progressBar.setVisible(false);
        progressBarLabel.setVisible(false);
        repeatScanButton.setEnabled(true);
    }

    public void setScanRunning() {
        progressBar.setVisible(true);
        progressBarLabel.setVisible(true);
        repeatScanButton.setEnabled(false);
    }

    public interface CloseListener {
        void onClose();
    }

    public interface ResultSelectedListener {
        void onResultSelected(AddDeviceDialog dialog, BleDevice bleDevice);
    }

    public interface RepeatScanListener {
        void onRepeatScan();
    }
}
