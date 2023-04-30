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
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import de.blitzdose.infinitrack.components.leaflet.LPolyline;
import de.blitzdose.infinitrack.data.entities.device.Device;
import de.blitzdose.infinitrack.data.entities.device.Location;
import de.blitzdose.infinitrack.data.services.DeviceService;
import de.blitzdose.infinitrack.gps.GPSUpdater;
import de.blitzdose.infinitrack.views.MainLayout;
import io.jenetics.jpx.GPX;
import io.jenetics.jpx.Speed;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.olli.FileDownloadWrapper;
import software.xdev.vaadin.maps.leaflet.flow.LMap;
import software.xdev.vaadin.maps.leaflet.flow.data.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@PageTitle("Map")
@Route(value = "map", layout = MainLayout.class)
@JsModule("leaflet/dist/leaflet.js")
@CssImport("leaflet/dist/leaflet.css")
@CssImport(value = "./themes/infinitrack/components/progressbar-color.css", themeFor = "vaadin-progress-bar")
public class MapView extends VerticalLayout {

    private final DeviceService deviceService;
    private final GPSUpdater gpsUpdater;
    
    private final List<Device> devices;

    private final TextField searchField = new TextField();
    private final UnorderedList cardList = new UnorderedList();
    private final Button startRecordingButton = new Button("Start recording");
    private final HorizontalLayout startRecordingButtonLayout = new HorizontalLayout();
    private final Scroller scroller = new Scroller();

    private final LMap lmap = new LMap();
    
    private final java.util.Map<Device, Button> locationToCardMap = new HashMap<>();

    private List<Device> filteredDevices;

    public MapView(@Autowired DeviceService deviceService, @Autowired GPSUpdater gpsUpdater) {
        addClassName("map-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        this.deviceService = deviceService;
        this.gpsUpdater = gpsUpdater;
        devices = deviceService.listWithLocations();

        createView();
        registerPollListener();
        updateCardList();
    }

    private void createView() {
        createSearchField();
        createCardList();
        createStartRecordingButton();
        createStartRecordingButtonLayout();
        createScroller();
        createMap();
        createSideBar();
    }

    private void createSearchField() {
        searchField.setPlaceholder("Search");
        searchField.setWidthFull();
        searchField.addClassNames(Padding.MEDIUM, BoxSizing.BORDER);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> updateFilter(searchField.getValue().toLowerCase()));
        searchField.setClearButtonVisible(true);
        searchField.setSuffixComponent(new Icon("lumo", "search"));
    }

    private void createCardList() {
        cardList.addClassNames("card-list", Gap.XSMALL, Display.FLEX, FlexDirection.COLUMN, ListStyleType.NONE,
                Margin.NONE, Padding.NONE);
    }

    private void createStartRecordingButton() {
        startRecordingButton.setWidthFull();
        if (gpsUpdater.isRecording()) {
            setRecordingButtonStarted();
        } else {
            setRecordingButtonStopped();
        }
        startRecordingButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> {
            if (gpsUpdater.isRecording()) {
                gpsUpdater.setRecording(false);
                setRecordingButtonStopped();
            } else {
                gpsUpdater.setRecording(true);
                setRecordingButtonStarted();
            }
        });
    }

    private void setRecordingButtonStarted() {
        startRecordingButton.setText("Stop recording");
        startRecordingButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        startRecordingButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        startRecordingButton.setIcon(VaadinIcon.STOP.create());
    }

    private void setRecordingButtonStopped() {
        startRecordingButton.setText("Start recording");
        startRecordingButton.removeThemeVariants(ButtonVariant.LUMO_ERROR);
        startRecordingButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        startRecordingButton.setIcon(VaadinIcon.PLAY.create());
    }

    private void createStartRecordingButtonLayout() {
        startRecordingButtonLayout.setWidthFull();
        startRecordingButtonLayout.addClassNames(Padding.MEDIUM, BoxSizing.BORDER);
        startRecordingButtonLayout.add(startRecordingButton);
    }

    private void createScroller() {
        scroller.addClassNames(Padding.Horizontal.MEDIUM, Width.FULL, BoxSizing.BORDER);
        scroller.setContent(cardList);
    }

    private void createSideBar() {
        VerticalLayout sidebar = new VerticalLayout();
        sidebar.setSpacing(false);
        sidebar.setPadding(false);

        sidebar.setWidth("auto");
        sidebar.setMinWidth("320px");
        sidebar.setHeightFull();
        sidebar.addClassNames("sidebar");
        sidebar.add(startRecordingButtonLayout, searchField, scroller);
        add(sidebar);
    }

    private void createMap() {
        lmap.setTileLayer(LTileLayer.DEFAULT_OPENSTREETMAP_TILE);
        lmap.setSizeFull();
        lmap.addMarkerClickListener((ComponentEventListener<LMap.MarkerClickEvent>) event -> {
            Device clickedDevice = devices.stream()
                    .filter(device1 -> device1.getAddress().equals(event.getTag()))
                    .findFirst()
                    .orElse(null);

            scrollToCard(clickedDevice);
        });
        
        this.centerMapDefault();
        this.updateFilter("");
        
        add(lmap);
    }

    private void centerMapDefault() {
        lmap.setCenter(new LCenter(52.358438, 4.881063, 4));
    }

    private void centerMapOn(Device device) {
        if (device.getLastLocation().getLatitude() == 0 && device.getLastLocation().getLongitude() == 0) {
            return;
        }
        lmap.setCenter(new LCenter(
                device.getLastLocation().getLatitude(),
                device.getLastLocation().getLongitude(), 18)
        );
    }

    private void scrollToCard(Device device) {
        if (device == null) {
            return;
        }
        locationToCardMap.get(device).scrollIntoView();
    }

    private void updateFilter(String filter) {
        filteredDevices = devices.stream()
                .filter(device -> (device.getName().toLowerCase().contains(filter)
                        || device.getAddress().toLowerCase().contains(filter)) 
                        && !device.getStatusGPS().equals("Offline"))
                .collect(Collectors.toList());

        removeAllMapComponents();

        this.filteredDevices.forEach(this::addDeviceToMap);
        updateCardList();
    }

    private void removeAllMapComponents() {
        List<LComponent> lComponents = lmap.getComponents().stream()
                .filter(lComponent -> lComponent instanceof LMarker || lComponent instanceof LPolyline)
                .toList();
        lmap.removeLComponents(lComponents);
    }

    public void addDeviceToMap(Device device) {
        if (addLMarkerToMap(device)) {
            addLPolylineToMap(device.getLocationHistory(), device.getColor());
        }
    }

    private boolean addLMarkerToMap(Device device) {
        LMarker lMarker = new LMarker(device.getLastLocation().getLatitude(), device.getLastLocation().getLongitude());
        if (lMarker.getLat() == 0 && lMarker.getLon() == 0) {
            return false;
        }
        lMarker.setDivIcon(createMarkerIcon(device.getColor()));
        lMarker.setTag(device.getAddress());

        lmap.addLComponents(lMarker);
        return true;
    }

    private void addLPolylineToMap(List<Location> locations, String color) {
        List<LPoint> lPoints = locations.stream()
                .map(location -> new LPoint(location.getLatitude(), location.getLongitude()))
                .toList();

        LPolyline lPolyline = new LPolyline(lPoints);
        lPolyline.setStrokeColor(color);
        lPolyline.setStrokeWeight(1);

        lmap.addLComponents(lPolyline);
    }

    private LDivIcon createMarkerIcon(String color) {
        Icon icon = new Icon(VaadinIcon.MAP_MARKER);
        icon.setColor(color);
        icon.setSize("46px");
        LDivIcon lDivIcon = new LDivIcon(icon.getElement().toString());
        lDivIcon.setIconSize(46, 46);
        lDivIcon.setClassName("map-icon");
        lDivIcon.setIconAnchor(23, 46);
        return lDivIcon;
    }

    private void updateCardList() {
        cardList.removeAll();
        locationToCardMap.clear();
        
        for (Device device : filteredDevices) {
            Span card = createDeviceCard(device);
            createDeviceCardButton(device, card);
        }
    }

    private void createDeviceCardButton(Device device, Span card) {
        Button button = new Button();
        button.addClassNames(Height.AUTO, Padding.MEDIUM);
        button.addClickListener(e -> centerMapOn(device));
        button.getElement().appendChild(card.getElement());
        cardList.add(new ListItem(button));
        locationToCardMap.put(device, button);
    }

    private Span createDeviceCard(Device device) {
        CardElements cardElements = createCardElements(device);

        Span card = new Span();
        card.addClassNames("card", Width.FULL, Display.FLEX, FlexDirection.COLUMN, AlignItems.START, Gap.XSMALL);
        card.add(
                cardElements.address(),
                cardElements.name(),
                cardElements.signal(),
                cardElements.satelliteCount(),
                cardElements.speed(),
                cardElements.buttonWrapper(),
                cardElements.progressBar()
        );
        return card;
    }

    private CardElements createCardElements(Device device) {
        Span address = createAddressSpan(device);
        Span name = createNameSpan(device);
        Span signal = createSignalSpan(device);
        Span satelliteCount = createSatelliteCountSpan(device);
        Span speedSpan = createSpeedSpan(device);
        FileDownloadWrapper buttonWrapper = createExportButton(device);
        ProgressBar progressBar = createProgressbar(device);

        return new CardElements(address, name, signal, satelliteCount, speedSpan, buttonWrapper, progressBar);
    }

    private record CardElements(Span address,
                                Span name,
                                Span signal,
                                Span satelliteCount,
                                Span speed,
                                FileDownloadWrapper buttonWrapper,
                                ProgressBar progressBar) {
    }

    private Span createNameSpan(Device device) {
        Span name = new Span(device.getName());
        name.addClassNames(FontSize.XLARGE, FontWeight.SEMIBOLD, TextColor.HEADER, Padding.Bottom.XSMALL);
        return name;
    }

    private Span createAddressSpan(Device device) {
        Span address = new Span(device.getAddress());
        address.addClassNames(TextColor.SECONDARY);
        return address;
    }


    private Span createSignalSpan(Device device) {
        Span signal = new Span("RSSI: " + device.getSignal());
        signal.addClassNames(TextColor.SECONDARY);
        return signal;
    }

    private Span createSatelliteCountSpan(Device device) {
        Span satelliteCount = new Span("Satellites: " + device.getLastLocation().getSatelliteCount());
        satelliteCount.addClassNames(TextColor.SECONDARY);
        return satelliteCount;
    }

    private Span createSpeedSpan(Device device) {
        Span speed = new Span("Speed: " + device.getLastLocation().getSpeed() + "km/h");
        speed.addClassNames(TextColor.SECONDARY);
        return speed;
    }

    private FileDownloadWrapper createExportButton(Device device) {
        Button exportButton = new Button("Export to GPX");
        exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        exportButton.setIcon(VaadinIcon.DOWNLOAD.create());
        FileDownloadWrapper buttonWrapper = new FileDownloadWrapper(
                new StreamResource(
                        device.getName().replace(" ", "_").toLowerCase() + ".gpx",
                        () -> createGPXFile(device))
        );
        buttonWrapper.wrapComponent(exportButton);

        buttonWrapper
                .getElement()
                .addEventListener("click", event -> {})
                .addEventData("event.stopPropagation()");
        return buttonWrapper;
    }

    private ByteArrayInputStream createGPXFile(Device device) {
        final GPX gpx = createGPX(device);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] gpxBytes = new byte[0];
        try {
            GPX.Writer.DEFAULT.write(gpx, outputStream);
            gpxBytes = outputStream.toByteArray();

            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ByteArrayInputStream(gpxBytes);
    }

    private GPX createGPX(Device device) {
        return GPX.builder()
                .creator("InfiniTrack")
                .addTrack(track -> track
                        .addSegment(segment -> device.getLocationHistory()
                                .forEach(location -> segment.addPoint(p ->
                                        p.lat(location.getLatitude())
                                                .lon(location.getLongitude())
                                                .ele(location.getAltitude())
                                                .speed(location.getSpeed(), Speed.Unit.KILOMETERS_PER_HOUR)
                                                .pdop((double) location.getPdop())
                                                .sat(location.getSatelliteCount())
                                                .time(location.getTimestamp())))))
                .build();
    }

    private ProgressBar createProgressbar(Device device) {
        ProgressBar progressBar = new ProgressBar();
        progressBar.setValue(1);
        progressBar.getStyle().set("--progress-color", device.getColor());
        return progressBar;
    }

    private void registerPollListener() {
        UI.getCurrent().setPollInterval(1000);
        UI.getCurrent().addPollListener((ComponentEventListener<PollEvent>) pollEvent -> {
            devices.clear();
            devices.addAll(deviceService.listWithLocations());
            updateFilter(searchField.getValue().toLowerCase());
        });
    }
}
