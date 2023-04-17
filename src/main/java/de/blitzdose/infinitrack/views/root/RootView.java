package de.blitzdose.infinitrack.views.root;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.blitzdose.infinitrack.views.MainLayout;

@PageTitle("InfiniTrack")
@Route(value = "", layout = MainLayout.class)
public class RootView extends VerticalLayout implements BeforeEnterObserver {

    public RootView() { }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.forwardToUrl("/map");
    }
}
