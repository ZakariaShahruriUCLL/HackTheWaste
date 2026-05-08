import L, { LatLngBoundsExpression } from "leaflet";

// Fix default marker icon paths when bundling - point to CDN.
delete (L.Icon.Default.prototype as { _getIconUrl?: unknown })._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
});

// Stadhuis van Leuven (city hall, Grote Markt) — exact city centre.
export const LEUVEN_CENTER: [number, number] = [50.8794, 4.7009];

// Soft bounding box around greater Leuven (Heverlee, Kessel-Lo, Wijgmaal,
// Wilsele) so users can pan the map but can't drift out to Brussels.
export const LEUVEN_BOUNDS: LatLngBoundsExpression = [
  [50.835, 4.6],
  [50.93, 4.78],
];

export const LEUVEN_MIN_ZOOM = 12;
export const LEUVEN_MAX_ZOOM = 18;
