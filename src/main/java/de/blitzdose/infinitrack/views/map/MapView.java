package de.blitzdose.infinitrack.views.map;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.PollEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.map.configuration.layer.TileLayer;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility.*;
import de.blitzdose.infinitrack.views.MainLayout;
import software.xdev.vaadin.maps.leaflet.flow.LMap;
import software.xdev.vaadin.maps.leaflet.flow.data.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@PageTitle("Map")
@Route(value = "map", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@JsModule("leaflet/dist/leaflet.js")
@CssImport("leaflet/dist/leaflet.css")
public class MapView extends HorizontalLayout {

    public static class Location {
        private int id;
        private String country;
        private String city;
        private String place;
        private double latitude;
        private double longitude;

        public Location(int id, String country, String city, String place, double latitude, double longitude) {
            this.id = id;
            this.country = country;
            this.city = city;
            this.place = place;
            this.latitude = latitude;
            this.longitude = longitude;

        }

        public int getId() {
            return id;
        }

        public String getCountry() {
            return country;
        }

        public String getCity() {
            return city;
        }

        public String getPlace() {
            return place;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

    }

    private static Location[] locations = new Location[]{
            new Location(1, "Netherlands", "test", "Van Gogh Museum", 52.358438, 4.881063),
            new Location(2, "Andorra", "Andorra la Vella", "Casa de la Vall", 42.506563, 1.520563),
            new Location(3, "Greece", "Athens", "Acropolis of Athens", 37.971563, 23.725687),
            new Location(4, "Serbia", "Belgrade", "Belgrade Fortress", 44.823062, 20.450688),
            new Location(5, "Germany", "Berlin", "Brandenburg Gate", 52.516312, 13.377688),
            new Location(6, "Switzerland", "Bern", "Bern Old Town", 46.948187, 7.450188),
            new Location(7, "Slovakia", "Bratislava", "Bratislava Castle", 48.142063, 17.100187),
            new Location(8, "Belgium", "Brussels", "Grand Place", 50.846812, 4.352438),
            new Location(9, "Romania", "Bucharest", "Romanian Athenaeum", 44.441312, 26.097313),
            new Location(10, "Hungary", "Budapest", "Fisherman's Bastion", 47.502187, 19.034813),
            new Location(11, "Moldova", "Chisinau", "Dendrarium Park", 47.032688, 28.810187),
            new Location(12, "Denmark", "Copenhagen", "The Little Mermaid", 55.692812, 12.599312),
            new Location(13, "Ireland", "Dublin", "Kilmainham Gaol", 53.341812, -6.309812),
            new Location(14, "Finland", "Helsinki", "Helsinki Olympic Stadium", 60.187188, 24.926812),
            new Location(15, "Ukraine", "Kiev", "Ukrainian Motherland Monument", 50.426562, 30.563063),
            new Location(16, "Portugal", "Lisbon", "Oceanário de Lisboa", 38.763562, -9.093687),
            new Location(17, "Slovenia", "Ljubljana", "Prešeren Square", 46.051437, 14.506063),
            new Location(18, "United Kingdom", "London", "London Eye", 51.503312, -0.119562),
            new Location(19, "Luxembourg", "Luxembourg", "Cathédrale Notre-Dame", 49.609562, 6.131562),
            new Location(20, "Spain", "Madrid", "Museo Nacional del Prado", 40.413812, -3.692188),
            new Location(21, "Belarus", "Minsk", "National Library of Republic of Belarus", 53.931438, 27.646187),
            new Location(22, "Monaco", "Monaco", "Saint Martin Gardens", 43.729937, 7.424188),
            new Location(23, "Cyprus", "Nicosia", "Selimiye Camii", 35.176563, 33.364562),
            new Location(24, "Greenland", "Nuuk", "Katuaq - Cultural Center", 64.177312, -51.738813),
            new Location(25, "Norway", "Oslo", "The Vigeland Park", 59.927062, 10.700812),
            new Location(26, "France", "Paris", "Eiffel Tower", 48.858312, 2.294437),
            new Location(27, "Montenegro", "Podgorica", "Dajbabe Monastery", 42.403937, 19.232438),
            new Location(28, "Czech Republic", "Prague", "Prague Astronomical Clock", 50.087063, 14.420687),
            new Location(29, "Iceland", "Reykjavik", "Hallgrímskirkja", 64.142062, -21.926563),
            new Location(30, "Latvia", "Riga", "House of the Black Heads", 56.947312, 24.106813),
            new Location(31, "Italy", "Rome", "Colosseum", 41.890187, 12.492188),
            new Location(32, "San Marino", "San Marino", "Public Palace of the Republic of San Marino", 43.936688,
                    12.446562),
            new Location(33, "Bosnia & Herzegovina", "Sarajevo", "Baščaršija", 43.859813, 18.431312),
            new Location(34, "North Macedonia", "Skopje", "Stone Bridge", 41.996938, 21.433062),
            new Location(35, "Bulgaria", "Sofia", "Bulevar Vitosha", 42.693062, 23.320188),
            new Location(36, "Sweden", "Stockholm", "Vasa Museum", 59.328063, 18.091437),
            new Location(37, "Estonia", "Tallinn", "Kadriorg Art Museum", 59.438562, 24.790937),
            new Location(38, "Albania", "Tirana", "Grand Park of Tirana", 41.312313, 19.825187),
            new Location(39, "Liechtenstein", "Vaduz", "Vaduz Castle", 47.139562, 9.524313),
            new Location(40, "Malta", "Valletta", "Upper Barrakka Gardens", 35.894937, 14.512187),
            new Location(41, "Holy See", "Vatican City", "Obelisk of St Peter's Square", 41.902187, 12.457313),
            new Location(42, "Austria", "Vienna", "Schönbrunn Palace", 48.185812, 16.312812),
            new Location(43, "Lithuania", "Vilnius", "Palace of the Grand Dukes of Lithuania", 54.686187, 25.289063),
            new Location(44, "Poland", "Warsaw", "Old Town Market Square", 52.249688, 21.012188),
            new Location(45, "Croatia", "Zagreb", "Park Maksimir", 45.824313, 16.017688)};

    private LMap lmap = new LMap();

    private UnorderedList cardList;
    private java.util.Map<Location, Button> locationToCard = new HashMap<>();

    private List<Location> filteredLocations;
    private java.util.Map<LMarker, Location> markerToLocation = new HashMap<>();

    public MapView() {
        addClassName("map-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        //map.getElement().setAttribute("theme", "borderless");
        //map.getElement().setAttribute("class", "map");
        //map.setHeightFull();

        VerticalLayout sidebar = new VerticalLayout();
        sidebar.setSpacing(false);
        sidebar.setPadding(false);

        sidebar.setWidth("auto");
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
        sidebar.add(searchField, scroller);
        scroller.setContent(cardList);

        configureMap();

        add(lmap, sidebar);

        updateCardList();

        UI.getCurrent().setPollInterval(1000);
        UI.getCurrent().addPollListener(new ComponentEventListener<PollEvent>() {
            @Override
            public void onComponentEvent(PollEvent pollEvent) {
                LComponent lComponent1 =  lmap.getComponents().stream().filter(lComponent -> lComponent instanceof LMarker).filter(lComponent -> ((LMarker) lComponent).getTag().equals("test")).findFirst().get();
                if (lComponent1 instanceof LMarker lMarker) {
                    lmap.removeLComponents(lMarker);
                    LMarkerGeometry geometry = lMarker.getGeometry();
                    LMarkerGeometry newGeometry = new LMarkerGeometry(geometry.getType(), lMarker.getLat(), lMarker.getLon() + 0.001);
                    lMarker.setGeometry(newGeometry);
                    lmap.addLComponents(lMarker);
                }
            }
        });
    }

    private void centerMapOn(Location location) {
        lmap.setCenter(new LCenter(location.getLatitude(), location.getLongitude(), 14));
    }

    private void scrollToCard(Location location) {
        locationToCard.get(location).scrollIntoView();
    }

    private void centerMapDefault() {
        lmap.setCenter(new LCenter(7, 55, 4));
    }

    private void configureMap() {
        lmap = new LMap(49.675126, 12.160733, 17);
        lmap.setTileLayer(LTileLayer.DEFAULT_OPENSTREETMAP_TILE);

        lmap.setSizeFull();

        this.centerMapDefault();
        this.updateFilter("");
    }

    private void updateCardList() {
        cardList.removeAll();
        locationToCard.clear();
        for (Location location : filteredLocations) {
            Button button = new Button();
            button.addClassNames(Height.AUTO, Padding.MEDIUM);
            button.addClickListener(e -> {
                centerMapOn(location);
            });

            Span card = new Span();
            card.addClassNames("card", Width.FULL, Display.FLEX, FlexDirection.COLUMN, AlignItems.START, Gap.XSMALL);
            Span country = new Span(location.getCountry());
            country.addClassNames(TextColor.SECONDARY);
            Span city = new Span(location.getCity());
            city.addClassNames(FontSize.XLARGE, FontWeight.SEMIBOLD, TextColor.HEADER, Padding.Bottom.XSMALL);
            Span place = new Span(location.getPlace());
            place.addClassNames(TextColor.SECONDARY);

            card.add(country, city, place);

            button.getElement().appendChild(card.getElement());
            cardList.add(new ListItem(button));
            locationToCard.put(location, button);
        }
    }

    private void updateFilter(String filter) {
        markerToLocation.clear();
        filteredLocations = Stream.of(locations)
                .filter(location -> location.place.toLowerCase().contains(filter)
                        || location.city.toLowerCase().contains(filter)
                        || location.country.toLowerCase().contains(filter))
                .collect(Collectors.toList());

        for (LMarker m : lmap.getComponents().stream().filter(lComponent -> lComponent instanceof LMarker).toArray(LMarker[]::new)) {
            lmap.removeLComponents(m);
        }


        String[] colors = new String[]{
                "#e27114",
                "#84000f",
                "#057e84",
                "#034582",
                "#932a04",
                "#910410",
                "#619b0a",
                "#d66413",
                "#c40eed",
                "#06a07c",
                "#962f03",
                "#0b1e7f",
                "#290a84",
                "#1b7a03",
                "#047c76",
                "#962706",
                "#026b11",
                "#e0e011",
                "#130060"
        };

        final int[] count = {0};


        this.filteredLocations.forEach((location) -> {
            LMarker lMarker = new LMarker(location.getLatitude(), location.getLongitude());
            lMarker.setIcon(new LIcon());
            lMarker.setPopup("Hallo");
            lMarker.setTag(location.city);

            //StreamResource streamResource = new StreamResource("location-dot-solid.svg",
            //        () -> getClass().getResourceAsStream(
            //                "/META-INF/resources/images/location-dot-solid.svg"));
            //com.vaadin.flow.component.map.configuration.style.Icon.Options markerIconOptions = new com.vaadin.flow.component.map.configuration.style.Icon.Options();
            //markerIconOptions.setImg(streamResource);
            //markerIconOptions.setColor(colors[count[0]++%19]);
            //markerIconOptions.setScale(0.25f);
            //com.vaadin.flow.component.map.configuration.style.Icon markerIcon = new com.vaadin.flow.component.map.configuration.style.Icon(markerIconOptions);
            //feature.setIcon(markerIcon);

            markerToLocation.put(lMarker, location);
            lmap.addLComponents(lMarker);
        });
        updateCardList();
    }
}
