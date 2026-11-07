"use client";

const UNIT_LOGO_URL = process.env.NEXT_PUBLIC_UNIT_LOGO_URL?.trim() || "/static/logo.jpg";

export function UnitLogo({ compact = false }: { compact?: boolean }) {
  return <span
    className={`unit-logo ${compact ? "unit-logo-compact" : ""} ${UNIT_LOGO_URL ? "has-image" : ""}`}
    style={UNIT_LOGO_URL ? { backgroundImage: `url("${UNIT_LOGO_URL.replaceAll('"', '%22')}")` } : undefined}
    role="img"
    aria-label="Logo Ban CHQS Phường Bắc Cam Ranh"
    title="Ban CHQS Phường Bắc Cam Ranh"
  />;
}

export function SiteFooter({ withSidebar = false }: { withSidebar?: boolean }) {
  return <footer className={`site-footer ${withSidebar ? "site-footer-sidebar" : ""}`}>
    <strong>Ban CHQS Phường Bắc Cam Ranh</strong>
    <span aria-hidden="true">·</span>
    <a href="https://www.bing.com/maps/default.aspx?v=2&pc=FACEBK&mid=8100&where1=%C4%90%C6%B0%E1%BB%9Dng%20Nguy%E1%BB%85n%20C%C3%B4ng%20Tr%E1%BB%A9%2C%20ph%C6%B0%E1%BB%9Dng%20B%E1%BA%AFc%20Cam%20Ranh%20&FORM=FBKPL1&mkt=en-US" target="_blank" rel="noreferrer">Đường Nguyễn Công Trứ, phường Bắc Cam Ranh</a>
    <span aria-hidden="true">·</span><a href="tel:02583973257">0258 3973 257</a>
    <span aria-hidden="true">·</span><a href="mailto:phucanhcr1995@gmail.com">phucanhcr1995@gmail.com</a>
    <span aria-hidden="true">·</span><a href="https://www.facebook.com/profile.php?id=100071454856063&locale=vi_VN" target="_blank" rel="noreferrer">Facebook chính thức</a>
  </footer>;
}
