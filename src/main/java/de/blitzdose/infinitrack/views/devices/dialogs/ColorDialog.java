package de.blitzdose.infinitrack.views.devices.dialogs;

import com.github.juchar.colorpicker.ColorPickerRaw;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;

class ColorDialog {

    private final String initialColor;
    private final String previousColor;

    private SaveListener saveListener;
    private CancelListener cancelListener;

    private final Dialog dialog;
    private ColorPickerRaw colorPicker;

    protected ColorDialog(String initialColor, String previousColor) {
        this.initialColor = initialColor;
        this.previousColor = previousColor;
        this.dialog = new Dialog();
        initialize();
    }

    private void initialize() {
        createColorPicker();
        createCancelButton();
        createSaveButton();
    }

    private void createColorPicker() {
        colorPicker = new ColorPickerRaw(initialColor, previousColor);
        colorPicker.setHslEnabled(false);
        colorPicker.setRgbEnabled(false);
        colorPicker.setAlphaEnabled(false);

        dialog.add(colorPicker);
    }

    private void createCancelButton() {
        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(clickEvent -> {
            if (cancelListener != null) {
                cancelListener.onCancel(ColorDialog.this);
            }
        });
        dialog.getFooter().add(cancelButton);
    }

    private void createSaveButton() {
        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(clickEvent -> {
            if (saveListener != null) {
                saveListener.onSave(ColorDialog.this, colorPicker.getValue());
            }
        });

        dialog.getFooter().add(saveButton);
    }

    protected ColorDialog setOnSaveListener(SaveListener saveListener) {
        this.saveListener = saveListener;
        return this;
    }

    protected ColorDialog setOnCancelListener(CancelListener cancelListener) {
        this.cancelListener = cancelListener;
        return this;
    }

    protected void show() {
        dialog.open();
    }

    protected void close() {
        dialog.close();
    }

    protected interface SaveListener {
        void onSave(ColorDialog colorDialog, String color);
    }

    protected interface CancelListener {
        void onCancel(ColorDialog colorDialog);
    }

}
