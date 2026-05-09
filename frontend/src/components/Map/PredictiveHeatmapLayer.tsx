import { useEffect, useRef } from "react";
import { useMap } from "react-leaflet";
import * as L from "leaflet";
import type { PredictPoint } from "../../api/types";

interface Props {
  points: PredictPoint[];
}

// Colormap: intensity 0→1 maps to green→yellow→orange→red
function toColor(t: number): [number, number, number] {
  if (t < 0.25) {
    const f = t / 0.25;
    return [Math.round(34 + f * (163 - 34)), Math.round(197 + f * (230 - 197)), Math.round(94 + f * (8 - 94))];
  }
  if (t < 0.5) {
    const f = (t - 0.25) / 0.25;
    return [Math.round(163 + f * (234 - 163)), Math.round(230 + f * (179 - 230)), Math.round(8 + f * (8 - 8))];
  }
  if (t < 0.75) {
    const f = (t - 0.5) / 0.25;
    return [Math.round(234 + f * (249 - 234)), Math.round(179 + f * (115 - 179)), Math.round(8 + f * (22 - 8))];
  }
  const f = (t - 0.75) / 0.25;
  return [Math.round(249 + f * (220 - 249)), Math.round(115 + f * (38 - 115)), Math.round(22 + f * (38 - 22))];
}

class CanvasHeatLayer extends L.Layer {
  private _canvas: HTMLCanvasElement | null = null;
  private _points: PredictPoint[] = [];
  private _leafletMap: L.Map | null = null;

  setPoints(points: PredictPoint[]) {
    this._points = points;
    this._draw();
  }

  onAdd(map: L.Map): this {
    this._leafletMap = map as L.Map;
    const pane = map.getPane("overlayPane")!;
    this._canvas = document.createElement("canvas");
    this._canvas.style.cssText =
      "position:absolute;top:0;left:0;pointer-events:none;";
    pane.appendChild(this._canvas);
    map.on("moveend zoomend resize", this._draw, this);
    this._draw();
    return this;
  }

  onRemove(map: L.Map): this {
    map.off("moveend zoomend resize", this._draw, this);
    this._canvas?.remove();
    this._canvas = null;
    this._leafletMap = null;
    return this;
  }

  private _draw() {
    if (!this._canvas || !this._leafletMap) return;
    const map = this._leafletMap;
    const canvas = this._canvas;

    const size = map.getSize();
    canvas.width = size.x;
    canvas.height = size.y;

    const topLeft = map.containerPointToLayerPoint([0, 0]);
    L.DomUtil.setPosition(canvas, topLeft);

    const ctx = canvas.getContext("2d")!;
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    if (!this._points.length) return;

    // Find max likelihood for normalisation
    const maxL = Math.max(...this._points.map((p) => p.likelihood));
    const scale = maxL > 0 ? 1 / maxL : 1;

    // Step 1 — draw intensity blobs on transparent canvas
    const RADIUS = 36;
    for (const p of this._points) {
      const px = map.latLngToContainerPoint([p.lat, p.lng]);
      const intensity = Math.min(1, p.likelihood * scale);
      const g = ctx.createRadialGradient(px.x, px.y, 0, px.x, px.y, RADIUS);
      g.addColorStop(0, `rgba(255,255,255,${intensity})`);
      g.addColorStop(1, "rgba(255,255,255,0)");
      ctx.beginPath();
      ctx.arc(px.x, px.y, RADIUS, 0, Math.PI * 2);
      ctx.fillStyle = g;
      ctx.fill();
    }

    // Step 2 — colourise by alpha value
    const imgData = ctx.getImageData(0, 0, canvas.width, canvas.height);
    const d = imgData.data;
    for (let i = 0; i < d.length; i += 4) {
      const alpha = d[i + 3] / 255;
      if (alpha > 0.01) {
        const [r, g, b] = toColor(alpha);
        d[i] = r;
        d[i + 1] = g;
        d[i + 2] = b;
        d[i + 3] = Math.round(alpha * 210);
      }
    }
    ctx.putImageData(imgData, 0, 0);
  }
}

export function PredictiveHeatmapLayer({ points }: Props) {
  const map = useMap();
  const layerRef = useRef<CanvasHeatLayer | null>(null);

  useEffect(() => {
    const layer = new CanvasHeatLayer();
    layer.addTo(map);
    layerRef.current = layer;
    return () => {
      map.removeLayer(layer);
      layerRef.current = null;
    };
  }, [map]);

  useEffect(() => {
    layerRef.current?.setPoints(points);
  }, [points]);

  return null;
}
