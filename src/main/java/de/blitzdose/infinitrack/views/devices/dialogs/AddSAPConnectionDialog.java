package de.blitzdose.infinitrack.views.devices.dialogs;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.TextRenderer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class AddSAPConnectionDialog {

    private SaveListener saveListener;
    private CancelListener cancelListener;

    private final Dialog dialog;
    private final VerticalLayout dialogLayout = new VerticalLayout();

    private final TextField urlTextField = new TextField("URL", "my.sapsailing.com", "");
    private final TextField leaderboardTextField = new TextField("Leaderboard name");
    private final TextField regattaSecret = new TextField("Regatta secret");

    private final ProgressBar progressBar = new ProgressBar();

    private final Select<JSONObject> teamSelect = new Select<>();

    protected AddSAPConnectionDialog() {
        this.dialog = new Dialog();

        initialize();
    }

    private void initialize() {
        dialog.setHeaderTitle("Add device");
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);

        createDialogLayout();
        createCancelButton();
        createNextButton();
    }

    private void createDialogLayout() {
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("min-width", "28rem").set("max-width", "100%");
        dialogLayout.setSpacing(false);
        dialogLayout.setPadding(false);

        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        dialogLayout.add(urlTextField);
        dialogLayout.add(leaderboardTextField);
        dialogLayout.add(regattaSecret);
        dialogLayout.add(progressBar);

        dialog.add(dialogLayout);
    }

    private void createCancelButton() {
        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(clickEvent -> {
            if (cancelListener != null) {
                cancelListener.onCancel(AddSAPConnectionDialog.this);
            }
        });
        dialog.getFooter().add(cancelButton);
    }

    private void createNextButton() {
        Button nextButton = new Button("Next");
        nextButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        nextButton.addClickListener(clickEvent -> {
            UI.getCurrent().access(() -> {
                urlTextField.setInvalid(false);
                leaderboardTextField.setInvalid(false);
                regattaSecret.setInvalid(false);
            });
            UI.getCurrent().access(() -> {
                if (leaderboardTextField.getValue().trim().isEmpty()) {
                    leaderboardTextField.setErrorMessage("Must be provided");
                    leaderboardTextField.setInvalid(true);
                    return;
                }

                if (regattaSecret.getValue().trim().isEmpty()) {
                    regattaSecret.setErrorMessage("Must be provided");
                    regattaSecret.setInvalid(true);
                    return;
                }

                requestTeams();
            });
        });

        dialog.getFooter().add(nextButton);
    }

    private void requestTeams() {
        progressBar.setVisible(true);

        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format("https://%s/sailingserver/api/v1/leaderboards/%s?secret=%s", urlTextField.getValue().trim(), leaderboardTextField.getValue().trim(), regattaSecret.getValue().trim())))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).get();

            if (response.statusCode() == 200) {
                JSONObject jsonObject = new JSONObject(response.body());

                JSONArray competitors = jsonObject.getJSONArray("competitors");

                renewDialogLayout(competitors);
            } else if (response.statusCode() == 401) {
                regattaSecret.setInvalid(true);
                regattaSecret.setErrorMessage("Wrong secret");
            } else if (response.statusCode() == 404) {
                leaderboardTextField.setInvalid(true);
                leaderboardTextField.setErrorMessage("Cannot find leaderboard");
            } else {
                urlTextField.setInvalid(true);
                urlTextField.setErrorMessage("Unknown error");
            }

            progressBar.setVisible(false);
        } catch (InterruptedException | ExecutionException | IllegalArgumentException e) {

            urlTextField.setInvalid(true);
            urlTextField.setErrorMessage("Unknown error");

            progressBar.setVisible(false);
        }
    }

    private void renewDialogLayout(JSONArray competitors) {
        dialogLayout.removeAll();
        dialog.getFooter().removeAll();
        createCancelButton();
        createSaveButton();

        createNewDialogLayout(competitors);
    }

    private void createNewDialogLayout(JSONArray competitors) {
        teamSelect.setLabel("Team");
        List<JSONObject> competitorsList = new ArrayList<>();

        for (int i=0; i<competitors.length(); i++) {
            competitorsList.add(competitors.getJSONObject(i));
        }

        teamSelect.setItems(competitorsList);
        teamSelect.setRenderer(new TextRenderer<>(item -> item.getString("name")));

        progressBar.setVisible(false);

        dialogLayout.add(teamSelect);
        dialogLayout.add(progressBar);
    }

    private void createSaveButton() {
        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(clickEvent -> {
            if (teamSelect.getValue() == null) {
                UI.getCurrent().access(() -> {
                   teamSelect.setInvalid(true);
                   teamSelect.setErrorMessage("Please select team");
                });
                return;
            }

            UUID uuid = UUID.randomUUID();

            JSONObject requestObject = new JSONObject();
            requestObject.put("competitorId", teamSelect.getValue().getString("id"));
            requestObject.put("deviceUuid", uuid.toString());
            requestObject.put("fromMillis", System.currentTimeMillis());
            requestObject.put("secret", regattaSecret.getValue().trim());

            saveListener.onSave(this, requestObject, String.format("https://%s/sailingserver/api/v1/leaderboards/%s/device_mappings/start", urlTextField.getValue().trim(), leaderboardTextField.getValue().trim()));
        });

        dialog.getFooter().add(saveButton);
    }

    protected AddSAPConnectionDialog setOnSaveListener(SaveListener saveListener) {
        this.saveListener = saveListener;
        return this;
    }

    protected AddSAPConnectionDialog setOnCancelListener(CancelListener cancelListener) {
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
        void onSave(AddSAPConnectionDialog dialog, JSONObject requestPayload, String sapUrl);
    }

    protected interface CancelListener {
        void onCancel(AddSAPConnectionDialog dialog);
    }
}
