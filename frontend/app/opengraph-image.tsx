import { ImageResponse } from "next/og";

export const size = { width: 1200, height: 630 };
export const contentType = "image/png";
export const alt = "Shortlist — sourcing that argues back";

/**
 * The link thumbnail, drawn here rather than shipped as a binary so it stays editable and
 * stays in step with the app's own palette.
 */
export default function OpengraphImage() {
  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          background: "#17212E",
          padding: "72px 80px",
          fontFamily: "sans-serif",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 20 }}>
          <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            <div style={{ width: 64, height: 8, borderRadius: 4, background: "#5B6B7B" }} />
            <div style={{ width: 64, height: 8, borderRadius: 4, background: "#1F6F5C" }} />
            <div style={{ width: 64, height: 8, borderRadius: 4, background: "#5B6B7B" }} />
          </div>
          <div style={{ fontSize: 30, color: "#8B98A5", letterSpacing: -0.4 }}>Shortlist</div>
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              fontSize: 84,
              color: "#ECEEF1",
              lineHeight: 1.05,
              letterSpacing: -2.5,
            }}
          >
            <span>Sourcing that</span>
            <span>argues back.</span>
          </div>
          <div style={{ fontSize: 30, color: "#8B98A5", maxWidth: 760, lineHeight: 1.35 }}>
            Describe the hire in a sentence. Tell it which candidates are wrong. Watch the brief
            change, and see exactly what moved.
          </div>
        </div>
      </div>
    ),
    size,
  );
}
