package de.blitzdose.infinitrack.views;


import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.PollEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.server.PWA;
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

    private H2 viewTitle;

    private Span baseStationSpan;
    private boolean openConnection = false;

    public MainLayout(@Autowired SerialCommunication communication) {
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent(communication);

        UI ui = UI.getCurrent();
        ui.setPollInterval(1000);
        ui.addPollListener(new ComponentEventListener<PollEvent>() {
            @Override
            public void onComponentEvent(PollEvent event) {
                baseStationSpan.getElement().setAttribute("theme", "badge " + (openConnection ? "success" : "error"));
            }
        });
    }

    private void addHeaderContent(SerialCommunication communication) {
        DrawerToggle toggle = new DrawerToggle();
        toggle.getElement().setAttribute("aria-label", "Menu toggle");

        viewTitle = new H2();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        addToNavbar(true, toggle, viewTitle);

        setTheme();

        baseStationSpan = new Span("Base station");
        baseStationSpan.getElement().setAttribute("theme", "badge error");
        baseStationSpan.getStyle().set("margin-left", "auto");

        if (communication.isConnected()) {
            baseStationSpan.getElement().setAttribute("theme", "badge success");
            openConnection = true;
        }
        communication.addOnConnectListener(new SerialCommunication.ConnectListener() {
            @Override
            public void connect() {
                openConnection = true;
            }
        });

        communication.addOnDisconnectListener(new SerialCommunication.DisconnectListener() {
            @Override
            public void disconnect() {
                openConnection = false;
            }
        });

        addToNavbar(baseStationSpan);

        Button button = createThemeSwitchButton();
        addToNavbar(true, button);
    }

    private void setTheme() {
        Cookie cookieDarkTheme = getCookieByName("dark_theme");

        ThemeList themeList = UI.getCurrent().getElement().getThemeList();
        if (cookieDarkTheme != null) {
            boolean darkTheme = Boolean.parseBoolean(cookieDarkTheme.getValue());
            if (darkTheme) {
                themeList.add(Lumo.DARK);
            } else {
                themeList.remove(Lumo.DARK);
            }
        } else {
            cookieDarkTheme = new Cookie("dark_theme", String.valueOf(themeList.contains(Lumo.DARK)));
            cookieDarkTheme.setPath(VaadinService.getCurrentRequest().getContextPath());
            VaadinService.getCurrentResponse().addCookie(cookieDarkTheme);
        }
    }

    private Button createThemeSwitchButton() {
        Button button = new Button("Dark Mode", click -> {
            ThemeList themeList = UI.getCurrent().getElement().getThemeList();

            if (themeList.contains(Lumo.DARK)) {
                themeList.remove(Lumo.DARK);
                click.getSource().setIcon(VaadinIcon.MOON_O.create());
                click.getSource().setText("Dark Mode");
            } else {
                themeList.add(Lumo.DARK);
                click.getSource().setIcon(VaadinIcon.SUN_O.create());
                click.getSource().setText("Light Mode");
            }

            Cookie cookieDarkTheme = getCookieByName("dark_theme");

            if (cookieDarkTheme != null) {
               cookieDarkTheme.setValue(String.valueOf(themeList.contains(Lumo.DARK)));
                VaadinService.getCurrentResponse().addCookie(cookieDarkTheme);
            }
        });
        button.getStyle().set("margin-left", "auto");
        button.getStyle().set("margin-right", "16px");

        ThemeList themeList = UI.getCurrent().getElement().getThemeList();

        button.setIcon(VaadinIcon.MOON_O.create());
        if (themeList.contains(Lumo.DARK)) {
            button.setIcon(VaadinIcon.SUN_O.create());
            button.setText("Light Mode");
        }
        return button;
    }

    private void addDrawerContent() {
        H1 appName = new H1("InfiniTrack");
        appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        Header header = new Header(appName);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    private AppNav createNavigation() {
        // AppNav is not yet an official component.
        // For documentation, visit https://github.com/vaadin/vcf-nav#readme
        AppNav nav = new AppNav();

        nav.addItem(new AppNavItem("Map", MapView.class, "la la-map"));
        nav.addItem(new AppNavItem("Base station", BaseStationView.class, "la la-code-branch"));
        nav.addItem(new AppNavItem("Devices", DevicesView.class, "la la-wifi"));

        return nav;
    }

    private Footer createFooter() {
        Footer layout = new Footer();

        return layout;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }

    private Cookie getCookieByName(String name) {
        // Fetch all cookies from the request
        Cookie[] cookies = VaadinService.getCurrentRequest().getCookies();

        // Iterate to find cookie by its name
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie;
            }
        }

        return null;
    }


}
