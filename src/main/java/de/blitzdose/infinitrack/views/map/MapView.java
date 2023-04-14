package de.blitzdose.infinitrack.views.map;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.PollEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import de.blitzdose.infinitrack.components.leaflet.LPolyline;
import de.blitzdose.infinitrack.data.entities.device.Device;
import de.blitzdose.infinitrack.data.services.DeviceService;
import de.blitzdose.infinitrack.views.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;
import software.xdev.vaadin.maps.leaflet.flow.LMap;
import software.xdev.vaadin.maps.leaflet.flow.data.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@PageTitle("Map")
@Route(value = "map", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@JsModule("leaflet/dist/leaflet.js")
@CssImport("leaflet/dist/leaflet.css")
@CssImport(value = "./themes/infinitrack/components/progressbar-color.css", themeFor = "vaadin-progress-bar")
public class MapView extends VerticalLayout {

    private LMap lmap = new LMap();

    private UnorderedList cardList;
    private java.util.Map<Device, Button> locationToCard = new HashMap<>();

    private List<Device> filteredDevices;
    private java.util.Map<LMarker, Device> markerToDevice = new HashMap<>();

    private final List<Device> devices;

    public MapView(@Autowired DeviceService deviceService) {
        addClassName("map-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        devices = deviceService.list();

        VerticalLayout sidebar = new VerticalLayout();
        sidebar.setSpacing(false);
        sidebar.setPadding(false);

        sidebar.setWidth("auto");
        sidebar.setHeightFull();
        sidebar.addClassNames("sidebar");
        TextField searchField = new TextField();
        searchField.setPlaceholder("Search");
        searchField.setWidthFull();
        searchField.addClassNames(Padding.MEDIUM, BoxSizing.BORDER);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> {
            updateFilter(searchField.getValue().toLowerCase());
        });
        searchField.setClearButtonVisible(true);
        searchField.setSuffixComponent(new Icon("lumo", "search"));

        Scroller scroller = new Scroller();
        scroller.addClassNames(Padding.Horizontal.MEDIUM, Width.FULL, BoxSizing.BORDER);

        cardList = new UnorderedList();
        cardList.addClassNames("card-list", Gap.XSMALL, Display.FLEX, FlexDirection.COLUMN, ListStyleType.NONE,
                Margin.NONE, Padding.NONE);

        Button startRecordingButton = new Button("Start recording");
        startRecordingButton.setWidthFull();
        startRecordingButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        startRecordingButton.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {
            @Override
            public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {

            }
        });
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setWidthFull();
        horizontalLayout.addClassNames(Padding.MEDIUM, BoxSizing.BORDER);
        horizontalLayout.add(startRecordingButton);

        sidebar.add(horizontalLayout, searchField, scroller);
        scroller.setContent(cardList);

        configureMap();

        add(lmap, sidebar);

        updateCardList();

        UI.getCurrent().setPollInterval(1000);
        UI.getCurrent().addPollListener(new ComponentEventListener<PollEvent>() {
            @Override
            public void onComponentEvent(PollEvent pollEvent) {
                devices.clear();
                devices.addAll(deviceService.list());
                updateFilter(searchField.getValue().toLowerCase());
            }
        });
    }

    private void centerMapOn(Device device) {
        if (device.getLastLocation().getLatitude() == 0 && device.getLastLocation().getLongitude() == 0) {
            return;
        }
        lmap.setCenter(new LCenter(device.getLastLocation().getLatitude(), device.getLastLocation().getLongitude(), 14));
    }

    private void scrollToCard(Device device) {
        locationToCard.get(device).scrollIntoView();
    }

    private void centerMapDefault() {
        lmap.setCenter(new LCenter(52.358438, 4.881063, 4));
    }

    private void configureMap() {
        lmap = new LMap(52.358438, 4.881063, 17);
        lmap.setTileLayer(LTileLayer.DEFAULT_OPENSTREETMAP_TILE);

        lmap.setSizeFull();

        LPolyline lPolyLine = new LPolyline(new LPoint(52.358438, 4.881063), new LPoint(52.516312, 13.377688), new LPoint(46.948187, 7.450188));
        lPolyLine.setStrokeColor("#ff0000");
        lmap.addLComponents(lPolyLine);

        this.centerMapDefault();
        this.updateFilter("");
    }

    private void updateCardList() {
        cardList.removeAll();
        locationToCard.clear();
        for (Device device : filteredDevices) {
            Button button = new Button();
            button.addClassNames(Height.AUTO, Padding.MEDIUM);
            button.addClickListener(e -> {
                centerMapOn(device);
            });

            Span card = new Span();
            card.addClassNames("card", Width.FULL, Display.FLEX, FlexDirection.COLUMN, AlignItems.START, Gap.XSMALL);
            Span address = new Span(device.getAddress());
            address.addClassNames(TextColor.SECONDARY);
            Span name = new Span(device.getName());
            name.addClassNames(FontSize.XLARGE, FontWeight.SEMIBOLD, TextColor.HEADER, Padding.Bottom.XSMALL);
            Span signal = new Span("RSSI: " + device.getSignal());
            signal.addClassNames(TextColor.SECONDARY);

            ProgressBar progressBar = new ProgressBar();
            progressBar.setValue(1);
            progressBar.getStyle().set("--progress-color", device.getColor());

            card.add(address, name, signal, progressBar);

            button.getElement().appendChild(card.getElement());
            cardList.add(new ListItem(button));
            locationToCard.put(device, button);
        }
    }

    private void updateFilter(String filter) {
        markerToDevice.clear();

        filteredDevices = devices.stream()
                .filter(device -> device.getName().toLowerCase().contains(filter)
                        || device.getAddress().toLowerCase().contains(filter))
                .collect(Collectors.toList());

        for (LMarker m : lmap.getComponents().stream().filter(lComponent -> lComponent instanceof LMarker).toArray(LMarker[]::new)) {
            lmap.removeLComponents(m);
        }

        final int[] count = {0};


        this.filteredDevices.forEach((device) -> {
            LMarker lMarker = new LMarker(device.getLastLocation().getLatitude(), device.getLastLocation().getLongitude());

            if (lMarker.getLat() == 0 && lMarker.getLon() == 0) {
                return;
            }

            Icon icon = new Icon(VaadinIcon.MAP_MARKER);
            icon.setColor(device.getColor());
            icon.setSize("46px");
            LDivIcon icon1 = new LDivIcon(icon.getElement().toString());
            icon1.setIconSize(46, 46);
            icon1.setClassName("map-icon");
            icon1.setIconAnchor(23, 46);
            lMarker.setDivIcon(icon1);

            lMarker.setTag(device.getAddress());

            markerToDevice.put(lMarker, device);
            lmap.addLComponents(lMarker);
            lmap.addMarkerClickListener(new ComponentEventListener<LMap.MarkerClickEvent>() {
                @Override
                public void onComponentEvent(LMap.MarkerClickEvent event) {
                    Device clickedDevice = devices.stream().filter(device1 -> device1.getAddress().equals(event.getTag())).findFirst().get();
                    scrollToCard(clickedDevice);
                }
            });
            count[0]++;
        });
        updateCardList();
    }
}
