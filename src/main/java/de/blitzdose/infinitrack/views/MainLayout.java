package de.blitzdose.infinitrack.views;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.PollEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.blitzdose.infinitrack.components.appnav.AppNav;
import de.blitzdose.infinitrack.components.appnav.AppNavItem;
import de.blitzdose.infinitrack.serial.SerialCommunication;
import de.blitzdose.infinitrack.views.devices.DevicesView;
import de.blitzdose.infinitrack.views.map.MapView;
import de.blitzdose.infinitrack.views.station.BaseStationView;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.Cookie;

/**
 * The main view is a top-level placeholder for other views.
 */
public class MainLayout extends AppLayout {

    private final SerialCommunication communication;

    private final H2 viewTitle = new H2();

    private final Span baseStationSpan = new Span("Base station");

    private final Button themeSwitchButton = new Button();

    private boolean openConnection = false;

    public MainLayout(@Autowired SerialCommunication communication) {
        this.communication = communication;

        createView();
        registerPollListener();
    }

    private void createView() {
        setPrimarySection(Section.DRAWER);
        createDrawer();
        createHeader();
        setTheme();
    }

    private void createDrawer() {
        Header header = createDrawerHeader();
        Scroller scroller = new Scroller(createDrawerNavigation());
        Footer footer = createDrawerFooter();
        addToDrawer(header, scroller, footer);
    }

    private Header createDrawerHeader() {
        H1 appName = new H1("InfiniTrack");
        appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        return new Header(appName);
    }

    private AppNav createDrawerNavigation() {
        AppNav nav = new AppNav();

        nav.addItem(new AppNavItem("Map", MapView.class, "la la-map"));
        nav.addItem(new AppNavItem("Base station", BaseStationView.class, "la la-code-branch"));
        nav.addItem(new AppNavItem("Devices", DevicesView.class, "la la-wifi"));

        return nav;
    }

    private Footer createDrawerFooter() {
        return new Footer();
    }

    private void createHeader() {
        createDrawerToggle();
        createViewTitle();
        createBaseStationBadge();
        createThemeSwitchButton();

        setBaseStationBadgeTheme();
        registerCommunicationListener();
    }

    private void createDrawerToggle() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.getElement().setAttribute("aria-label", "Menu toggle");
        addToNavbar(true, toggle);
    }

    private void createViewTitle() {
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        addToNavbar(true, viewTitle);
    }

    private void createBaseStationBadge() {
        baseStationSpan.getElement().setAttribute("theme", "badge error");
        baseStationSpan.setWidth("fit-content");
        baseStationSpan.getStyle().set("position", "absolute");
        baseStationSpan.getStyle().set("margin-left", "auto");
        baseStationSpan.getStyle().set("margin-right", "auto");
        baseStationSpan.getStyle().set("left", "0");
        baseStationSpan.getStyle().set("right", "0");

        addToNavbar(baseStationSpan);
    }

    private void createThemeSwitchButton() {
        themeSwitchButton.setText("Dark Mode");
        themeSwitchButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) click -> {
            ThemeList themeList = UI.getCurrent().getElement().getThemeList();
            setDarkMode(!themeList.contains(Lumo.DARK));
        });
        themeSwitchButton.getStyle().set("margin-left", "auto");
        themeSwitchButton.getStyle().set("margin-right", "16px");

        addToNavbar(true, themeSwitchButton);
    }

    private void setDarkMode(boolean darkTheme) {
        ThemeList themeList = UI.getCurrent().getElement().getThemeList();
        if (darkTheme) {
            themeList.add(Lumo.DARK);
            themeSwitchButton.setIcon(VaadinIcon.SUN_O.create());
            themeSwitchButton.setText("Light Mode");
        } else {
            themeList.remove(Lumo.DARK);
            themeSwitchButton.setIcon(VaadinIcon.MOON_O.create());
            themeSwitchButton.setText("Dark Mode");
        }
        setDarkModeCookie(darkTheme);
    }

    private void setDarkModeCookie(boolean darkTheme) {
        Cookie cookie = getDarkModeCookie();
        if (cookie == null) {
            cookie = new Cookie("dark_theme", String.valueOf(darkTheme));
        }
        cookie.setValue(String.valueOf(darkTheme));
        cookie.setPath(VaadinService.getCurrentRequest().getContextPath());
        cookie.setMaxAge(Integer.MAX_VALUE);
        VaadinService.getCurrentResponse().addCookie(cookie);
    }

    private Cookie getDarkModeCookie() {
        Cookie[] cookies = VaadinService.getCurrentRequest().getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("dark_theme")) {
                return cookie;
            }
        }
        return null;
    }

    private void setBaseStationBadgeTheme() {
        if (communication.isConnected()) {
            baseStationSpan.getElement().setAttribute("theme", "badge success");
            openConnection = true;
        }
    }

    private void setTheme() {
        Cookie cookieDarkTheme = getDarkModeCookie();
        setDarkMode(cookieDarkTheme != null && Boolean.parseBoolean(cookieDarkTheme.getValue()));
    }

    private void registerCommunicationListener() {
        communication.addOnConnectListener(() -> openConnection = true);
        communication.addOnDisconnectListener(() -> openConnection = false);
    }

    private void registerPollListener() {
        UI ui = UI.getCurrent();
        ui.setPollInterval(1000);
        ui.addPollListener((ComponentEventListener<PollEvent>) event ->
                baseStationSpan
                        .getElement()
                        .setAttribute("theme", "badge " + (openConnection ? "success" : "error")));
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }
}
