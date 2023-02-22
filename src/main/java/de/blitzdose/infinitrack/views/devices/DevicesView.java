package de.blitzdose.infinitrack.views.devices;

import com.github.juchar.colorpicker.ColorPickerRaw;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.FocusNotifier;
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
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.gridpro.GridPro;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.map.configuration.Coordinate;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.data.selection.SelectionEvent;
import com.vaadin.flow.data.selection.SelectionListener;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.blitzdose.infinitrack.data.entities.Device;
import de.blitzdose.infinitrack.views.MainLayout;
import org.apache.commons.lang3.StringUtils;
import org.vaadin.addon.stefan.clipboard.ClientsideClipboard;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@PageTitle("Devices")
@Route(value = "devices", layout = MainLayout.class)
@NpmPackage(value = "@datadobi/color-picker", version = "2.0.0-beta.4-datadobi8")
@JsModule("@datadobi/color-picker/color-picker.js")
public class DevicesView extends Div {

    private GridPro<Device> grid;
    GridListDataView<Device> gridListDataView;

    public DevicesView() {
        addClassName("devices-view");
        setSizeFull();
        createGrid();

        TextField searchField = new TextField();
        searchField.setWidth("100%");
        searchField.setPlaceholder("Search");
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> gridListDataView.refreshAll());

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

        Button primaryButton = new Button("Add Device", new Icon(VaadinIcon.PLUS));
        primaryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        primaryButton.setMinWidth("auto");

        HorizontalLayout horizontalLayout = new HorizontalLayout(searchField, primaryButton);
        horizontalLayout.setWidth("100%");

        VerticalLayout layout = new VerticalLayout(horizontalLayout, grid);
        layout.setHeightFull();
        //layout.setPadding(false);

        add(layout);
    }

    private void createGrid() {
        createGridComponent();
        addColumnsToGrid();
    }

    private void createGridComponent() {
        grid = new GridPro<>();
        grid.setSelectionMode(SelectionMode.SINGLE);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COLUMN_BORDERS);
        grid.setWidthFull();

        List<Device> devices = getDevices();
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

        TextField textField = new TextField("Color");
        textField.setValue(device.getColor());
        textField.getStyle().set("flex-grow", "1");
        colorLayout.add(textField);
        textField.addFocusListener(new ComponentEventListener<FocusNotifier.FocusEvent<TextField>>() {
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
        textField.addValueChangeListener(event -> icon.setColor(event.getValue()));

        colorLayout.add(icon);
        dialogLayout.add(colorLayout);

        TextField versionTextField = new TextField("Firmware version");
        versionTextField.setReadOnly(true);
        versionTextField.setValue(device.getFirmwareVersion());
        dialogLayout.add(versionTextField);

        TextField lastLocationTextField = new TextField("Last known location");
        lastLocationTextField.setReadOnly(true);
        lastLocationTextField.setValue(device.getCoordinate() != null ? device.getCoordinateAsString() : "Unknown");
        Span copyIcon = new Span();
        copyIcon.setClassName("la la-copy");
        lastLocationTextField.setSuffixComponent(copyIcon);
        lastLocationTextField.getElement().addEventListener("click", clickEvent -> {
            ClientsideClipboard.writeToClipboard(lastLocationTextField.getValue(), successful ->
                    Notification.show(successful ? "Copied to clipboard" : "Could not write to clipboard")
            );

        });
        dialogLayout.add(lastLocationTextField);

        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("min-width", "28rem").set("max-width", "100%");
        dialogLayout.setSpacing(false);
        dialogLayout.setPadding(false);

        deviceDialog.add(dialogLayout);


        Button closeButton = new Button(new Icon("lumo", "cross"),
                (e) -> deviceDialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        deviceDialog.getHeader().add(closeButton);


        Button saveButton = new Button("Save", e -> System.out.println("saved"));
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
    }

    private void deleteDevice(Device device) {

    }

    private void addColumnsToGrid() {
        createIdColumn();
        createColorColumn();
        createNameColumn();
        createSignalColumn();
        createStatusColumn();
    }

    private void createIdColumn() {
        Grid.Column<Device> idColumn = grid
                .addColumn(new NumberRenderer<>(Device::getId, NumberFormat.getNumberInstance(Locale.GERMANY)))
                .setComparator(Device::getId).setHeader("ID")
                .setFlexGrow(0);
    }

    private void createColorColumn() {
        Grid.Column<Device> colorColumn = grid
                .addColumn(new ComponentRenderer<>(device -> {
                    Icon icon = new Icon(VaadinIcon.CIRCLE);
                    icon.setColor(device.getColor());
                    return icon;
                }))
                .setTextAlign(ColumnTextAlign.CENTER)
                .setHeader("Color")
                .setFlexGrow(0);
    }

    private void createNameColumn() {
        Grid.Column<Device> nameColumn = grid
                .addColumn(new TextRenderer<>(Device::getName))
                .setComparator(Device::getName).setHeader("Name");
    }

    private void createSignalColumn() {
        Grid.Column<Device> signalColumn = grid
                .addColumn(new NumberRenderer<>(Device::getSignal, "%.2f dB", Locale.GERMANY))
                .setComparator(Device::getSignal).setHeader("Signal");
    }

    private void createStatusColumn() {
        Grid.Column<Device> statusColumn = grid.addColumn(new ComponentRenderer<>(device -> {
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
        return Arrays.asList(
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

    private Device createDevice(int id, String name, double amount, String status, String firmwareVersion) {
        Device c = new Device();
        c.setId(id);
        c.setColor("#000000");
        c.setCoordinate(new Coordinate(9.070210, 48.514922));
        c.setName(name);
        c.setSignal(amount);
        c.setStatus(status);
        c.setFirmwareVersion(firmwareVersion);

        return c;
    }

    private boolean matchesTerm(String value, String searchTerm) {
        return value.toLowerCase().contains(searchTerm.toLowerCase());
    }

};
