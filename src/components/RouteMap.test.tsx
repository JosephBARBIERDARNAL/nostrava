import { cleanup, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { RouteMap } from "@/components/RouteMap";
import type { TrackPoint } from "@/lib/api";

const leaflet = vi.hoisted(() => {
  const mapInstance = {
    fitBounds: vi.fn(),
    invalidateSize: vi.fn(),
    remove: vi.fn(),
  };
  const tileLayerInstance = {
    addTo: vi.fn(() => tileLayerInstance),
  };
  const polylineInstance = {
    addTo: vi.fn(() => polylineInstance),
    getBounds: vi.fn(() => "bounds"),
    remove: vi.fn(),
  };
  const circleMarkerInstance = {
    addTo: vi.fn(() => circleMarkerInstance),
    remove: vi.fn(),
  };

  return {
    map: vi.fn(() => mapInstance),
    tileLayer: vi.fn(() => tileLayerInstance),
    polyline: vi.fn(() => polylineInstance),
    circleMarker: vi.fn(() => circleMarkerInstance),
    mapInstance,
    tileLayerInstance,
    polylineInstance,
    circleMarkerInstance,
  };
});

vi.mock("leaflet", () => leaflet);

beforeEach(() => {
  vi.clearAllMocks();
});

afterEach(() => {
  cleanup();
});

function point(lat: number, lng: number, paused = false): TrackPoint {
  return {
    timestamp_ms: 1,
    lat,
    lng,
    altitude_m: null,
    accuracy_m: 5,
    paused,
  };
}

describe("RouteMap", () => {
  it("shows an empty state until there are two active points", () => {
    render(<RouteMap points={[point(48.8566, 2.3522)]} />);

    expect(screen.getByTestId("route-map-empty")).toHaveTextContent("No track recorded");
    expect(leaflet.map).not.toHaveBeenCalled();
  });

  it("renders an OSM map with a route and markers", async () => {
    render(<RouteMap points={[point(48.8566, 2.3522), point(48.857, 2.353)]} />);

    expect(screen.getByRole("img", { name: "Route map" })).toBeInTheDocument();
    await waitFor(() => expect(leaflet.map).toHaveBeenCalledTimes(1));
    expect(leaflet.tileLayer).toHaveBeenCalledWith(
      "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
      expect.objectContaining({ maxZoom: 19 }),
    );
    expect(leaflet.polyline).toHaveBeenCalledWith(
      [
        [48.8566, 2.3522],
        [48.857, 2.353],
      ],
      expect.objectContaining({ weight: 4 }),
    );
    expect(leaflet.circleMarker).toHaveBeenCalledTimes(2);
    expect(leaflet.mapInstance.fitBounds).toHaveBeenCalledWith("bounds", expect.any(Object));
  });

  it("ignores paused points when building the route", async () => {
    render(
      <RouteMap
        points={[
          point(48.8566, 2.3522),
          point(49.5, 2.8, true),
          point(48.857, 2.353),
        ]}
      />,
    );

    await waitFor(() => expect(leaflet.polyline).toHaveBeenCalled());
    expect(leaflet.polyline).toHaveBeenCalledWith(
      [
        [48.8566, 2.3522],
        [48.857, 2.353],
      ],
      expect.any(Object),
    );
  });

  it("uses the live route accessible name", () => {
    render(<RouteMap points={[point(48.8566, 2.3522), point(48.857, 2.353)]} live />);

    expect(screen.getByRole("img", { name: "Live route map" })).toBeInTheDocument();
  });
});
