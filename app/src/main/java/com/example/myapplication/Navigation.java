package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.EdgeInsets;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.LocationPuck2D;
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin;
import com.mapbox.maps.plugin.animation.CameraAnimationsUtils;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils;

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI;
import com.mapbox.navigation.base.extensions.RouteOptionsExtensionsKt;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.base.route.NavigationRoute;
import com.mapbox.navigation.base.route.NavigationRouterCallback;
import com.mapbox.navigation.base.route.RouterFailure;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.directions.session.RoutesObserver;
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp;
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver;
import com.mapbox.navigation.core.lifecycle.MapboxNavigationProvider;
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver;
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper;
import com.mapbox.navigation.core.trip.session.LocationMatcherResult;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.ui.maps.camera.NavigationCamera;
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource;
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider;
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi;
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView;
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions;
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions;

import java.util.Collections;
import java.util.List;

public class Navigation extends ComponentActivity {

    private MapView mapView;
    private MapboxNavigationViewportDataSource viewportDataSource;
    private NavigationCamera navigationCamera;
    private MapboxRouteLineApi routeLineApi;
    private MapboxRouteLineView routeLineView;
    private ReplayProgressObserver replayProgressObserver;
    private final NavigationLocationProvider navigationLocationProvider = new NavigationLocationProvider();
    private final ReplayRouteMapper replayRouteMapper = new ReplayRouteMapper();
    private MapboxNavigation mapboxNavigation;

    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                Boolean coarse = permissions.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                if (Boolean.TRUE.equals(coarse)) {
                    initializeMapComponents();
                } else {
                    Toast.makeText(this, "Location permissions denied. Please enable permissions in settings.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            initializeMapComponents();
        } else {
            locationPermissionRequest.launch(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    private void initializeMapComponents() {
        mapView = new MapView(this);
        setContentView(mapView);

        mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                .center(Point.fromLngLat(-122.43539772352648, 37.77440680146262))
                .zoom(14.0)
                .build());

        LocationComponentPlugin locationComponent = LocationComponentUtils.getLocationComponent(mapView);
        locationComponent.setLocationProvider(navigationLocationProvider);
        locationComponent.setLocationPuck(new LocationPuck2D());
        locationComponent.setEnabled(true);

        float pixelDensity = getResources().getDisplayMetrics().density;

        viewportDataSource = new MapboxNavigationViewportDataSource(mapView.getMapboxMap());
        viewportDataSource.setFollowingPadding(new EdgeInsets(
                180.0 * pixelDensity,
                40.0 * pixelDensity,
                150.0 * pixelDensity,
                40.0 * pixelDensity
        ));

        CameraAnimationsPlugin cameraPlugin = CameraAnimationsUtils.getCamera(mapView);
        navigationCamera = new NavigationCamera(mapView.getMapboxMap(), cameraPlugin, viewportDataSource);

        routeLineApi = new MapboxRouteLineApi(new MapboxRouteLineApiOptions.Builder().build());
        routeLineView = new MapboxRouteLineView(new MapboxRouteLineViewOptions.Builder(this).build());

        initNavigation();
    }

    private final RoutesObserver routesObserver = routeUpdateResult -> {
        if (!routeUpdateResult.getNavigationRoutes().isEmpty()) {
            routeLineApi.setNavigationRoutes(routeUpdateResult.getNavigationRoutes(), result -> {
                Style style = mapView.getMapboxMap().getStyle();
                if (style != null) {
                    routeLineView.renderRouteDrawData(style, result);
                }
            });

            viewportDataSource.onRouteChanged(routeUpdateResult.getNavigationRoutes().get(0));
            viewportDataSource.evaluate();
            navigationCamera.requestNavigationCameraToOverview();
        }
    };

    private final LocationObserver locationObserver = new LocationObserver() {
        @Override
        public void onNewRawLocation(@NonNull Location rawLocation) {}

        @Override
        public void onNewLocationMatcherResult(@NonNull LocationMatcherResult locationMatcherResult) {
            android.location.Location enhancedLocation = locationMatcherResult.getEnhancedLocation();
            navigationLocationProvider.changePosition(enhancedLocation, locationMatcherResult.getKeyPoints());
            viewportDataSource.onLocationChanged(enhancedLocation);
            viewportDataSource.evaluate();
            navigationCamera.requestNavigationCameraToFollowing();
        }
    };

    @OptIn(markerClass = ExperimentalPreviewMapboxNavigationAPI.class)
    private void initNavigation() {
        MapboxNavigationApp.setup(new NavigationOptions.Builder(this).build());
        mapboxNavigation = MapboxNavigationProvider.retrieve();

        mapboxNavigation.registerRoutesObserver(routesObserver);
        mapboxNavigation.registerLocationObserver(locationObserver);

        replayProgressObserver = new ReplayProgressObserver(mapboxNavigation.getMapboxReplayer());
        mapboxNavigation.registerRouteProgressObserver(replayProgressObserver);
        mapboxNavigation.startReplayTripSession();

        LocationComponentPlugin locationComponent = LocationComponentUtils.getLocationComponent(mapView);
        locationComponent.setLocationProvider(navigationLocationProvider);
        locationComponent.setLocationPuck(LocationComponentUtils.createDefault2DPuck());
        locationComponent.setEnabled(true);

        Point origin = Point.fromLngLat(-122.43539772352648, 37.77440680146262);
        Point destination = Point.fromLngLat(-122.42409811526268, 37.76556957793795);

        mapboxNavigation.requestRoutes(
                RouteOptionsExtensionsKt.applyDefaultNavigationOptions(
                        RouteOptionsExtensionsKt.coordinatesList(
                                RouteOptions.builder(), List.of(origin, destination)
                        ).layersList(List.of(mapboxNavigation.getZLevel(), null))
                ).build(),
                new NavigationRouterCallback() {
                    @Override
                    public void onCanceled(@NonNull com.mapbox.api.directions.v5.models.RouteOptions routeOptions, @NonNull String routerOrigin) {}

                    @Override
                    public void onFailure(@NonNull List<RouterFailure> reasons, @NonNull com.mapbox.api.directions.v5.models.RouteOptions routeOptions) {}

                    @Override
                    public void onRoutesReady(@NonNull List<NavigationRoute> routes, @NonNull String routerOrigin) {
                        mapboxNavigation.setNavigationRoutes(routes);

                        List<com.mapbox.navigation.core.replay.route.ReplayEventBase> replayEvents = replayRouteMapper.mapDirectionsRouteGeometry(
                                routes.get(0).getDirectionsRoute()
                        );

                        mapboxNavigation.getMapboxReplayer().pushEvents(replayEvents);
                        mapboxNavigation.getMapboxReplayer().seekTo(replayEvents.get(0));
                        mapboxNavigation.getMapboxReplayer().play();
                    }
                }
        );
    }
}
