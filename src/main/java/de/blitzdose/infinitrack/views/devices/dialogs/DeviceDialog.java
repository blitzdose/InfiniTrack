package de.blitzdose.infinitrack.views.devices.dialogs;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import de.blitzdose.infinitrack.data.entities.device.Device;
import org.vaadin.olli.ClipboardHelper;

import java.util.Locale;

public class DeviceDialog {
    
    private final Dialog dialog;
    private final Device device;

    private SaveListener saveListener;
    private DeleteListener deleteListener;

    private final VerticalLayout dialogLayout = new VerticalLayout();
    private final TextField nameTextField = new TextField("Name");
    private final TextField colorTextField = new TextField("Color");
    private final TextField sapTextField = new TextField("SAP Device ID");
    private final Icon colorIcon = new Icon(VaadinIcon.CIRCLE);
    
    public DeviceDialog(Device device) {
        this.device = device;
        this.dialog = new Dialog();
        initialize();
    }
    
    private void initialize() {
        createDialogLayout();

        createCloseButton();

        createDeleteButton();
        createSaveButton();
    }

    private void createSaveButton() {
        Button saveButton = new Button("Save", e -> {
            device.setName(nameTextField.getValue());
            device.setColor(colorTextField.getValue());
            device.setSapUUID(sapTextField.getValue().trim().isEmpty() ? null : sapTextField.getValue().trim());
            if (saveListener != null) {
                saveListener.onSave(DeviceDialog.this, device);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(saveButton);
    }

    private void createDeleteButton() {
        Button deleteButton = new Button("Delete", click -> showConfirmDialog());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        deleteButton.getStyle().set("margin-right", "auto");

        dialog.getFooter().add(deleteButton);
    }

    private void showConfirmDialog() {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader(String.format("Delete device %d?", device.getId()));
        confirmDialog.setText(
                "Are you sure you want to delete the Device? This cannot be undone.");
        confirmDialog.setRejectable(true);
        confirmDialog.setRejectText("No");
        confirmDialog.addRejectListener(event -> confirmDialog.close());
        confirmDialog.setConfirmText("Yes");
        confirmDialog.addConfirmListener(event -> {
            if (deleteListener != null) {
                deleteListener.onDelete(DeviceDialog.this, device);
            }
        });

        confirmDialog.open();
    }

    private void createCloseButton() {
        Button closeButton = new Button(new Icon("lumo", "cross"),
                (e) -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getHeader().add(closeButton);
    }

    private void createDialogLayout() {
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("min-width", "28rem").set("max-width", "100%");
        dialogLayout.setSpacing(false);
        dialogLayout.setPadding(false);

        setDialogTitle();
        createStatusSpan();
        createNameTextField();
        createColorLayout();
        createSAPIDTextField();
        createAddressTextField();
        createLastLocationLayout();

        dialog.add(dialogLayout);
    }

    private void createLastLocationLayout() {
        TextField lastLocationTextField = createLastLocationTextfield();
        ClipboardHelper clipboardHelper = createCopyIcon(lastLocationTextField);

        HorizontalLayout horizontalLayout = new HorizontalLayout(lastLocationTextField, clipboardHelper);
        horizontalLayout.setAlignItems(FlexComponent.Alignment.END);
        dialogLayout.add(horizontalLayout);
    }

    private ClipboardHelper createCopyIcon(TextField lastLocationTextField) {
        Span copyIcon = new Span();
        copyIcon.setClassName("la la-copy");
        Button button = new Button("Copy to clipboard", copyIcon);
        ClipboardHelper clipboardHelper = new ClipboardHelper(lastLocationTextField.getValue(), button);
        clipboardHelper.getElement().addEventListener("click", event -> Notification.show("Copied to clipboard"));
        return clipboardHelper;
    }

    private TextField createLastLocationTextfield() {
        TextField lastLocationTextField = new TextField("Last known location");
        lastLocationTextField.setReadOnly(true);
        lastLocationTextField.setValue(device.getLastLocation() != null ? String.format(Locale.ROOT, "%f, %f", device.getLastLocation().getLatitude(), device.getLastLocation().getLatitude()) : "Unknown");
        lastLocationTextField.setWidthFull();
        return lastLocationTextField;
    }

    private void createAddressTextField() {
        TextField addressTextField = new TextField("Address");
        addressTextField.setReadOnly(true);
        addressTextField.setValue(device.getAddress());
        dialogLayout.add(addressTextField);
    }

    private void createSAPIDTextField() {
        sapTextField.setValue(device.getSapUUID() == null ? "" : device.getSapUUID());
        dialogLayout.add(sapTextField);
    }

    private void createColorLayout() {
        createColorIcon();
        createColorTextField();

        HorizontalLayout colorLayout = new HorizontalLayout();
        colorLayout.add(colorTextField, colorIcon);
        dialogLayout.add(colorLayout);
    }

    private void createColorIcon() {
        colorIcon.setColor(device.getColor());
        colorIcon.getStyle().set("align-self", "end");
        colorIcon.getStyle().set("margin", "var(--lumo-space-xs) 0");
        colorIcon.getStyle().set("height", "36px");
        colorIcon.getStyle().set("width", "36px");

        colorIcon.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> openColorDialog());
    }

    private void openColorDialog() {
        new ColorDialog(colorTextField.getValue(), colorTextField.getValue())
                .setOnSaveListener((colorDialog, color) -> {
                    colorTextField.setValue(color);
                    colorDialog.close();
                })
                .setOnCancelListener(ColorDialog::close)
                .show();
    }

    private void createColorTextField() {
        colorTextField.setValue(device.getColor());
        colorTextField.getStyle().set("flex-grow", "1");
        colorTextField.addValueChangeListener(event -> colorIcon.setColor(event.getValue()));
        colorTextField.setValueChangeMode(ValueChangeMode.EAGER);
    }

    private void createNameTextField() {
        nameTextField.setValue(device.getName());
        dialogLayout.add(nameTextField);
    }

    private void createStatusSpan() {
        Span span = new Span();
        span.setText(device.getStatus());
        span.getElement().setAttribute("theme", "badge " + (device.getStatus().equalsIgnoreCase("connected") ? "success" : "error"));
        dialogLayout.add(span);
    }

    private void setDialogTitle() {
        dialog.setHeaderTitle(String.format("Device %d", device.getId()));
    }

    public void show() {
        this.dialog.open();
    }

    public void close() {
        this.dialog.close();
    }

    public DeviceDialog setSaveListener(SaveListener saveListener) {
        this.saveListener = saveListener;
        return this;
    }

    public DeviceDialog setDeleteListener(DeleteListener deleteListener) {
        this.deleteListener = deleteListener;
        return this;
    }

    public interface SaveListener {
        void onSave(DeviceDialog deviceDialog, Device device);
    }

    public interface DeleteListener {
        void onDelete(DeviceDialog deviceDialog, Device device);
    }
}
