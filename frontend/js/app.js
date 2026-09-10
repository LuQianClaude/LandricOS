/* ==========================================================================
   LandricsVilla - App shell builder, router, auth state, shared utilities.
   Per-page architecture: each page is a full HTML doc carrying a
   <template id="page-content"> fragment. init() builds the shared shell
   (sidebar + bottom nav), clones the template into the view, then runs the
   page init registered on window.LVPages[data-page].
   No build step, no framework. No emojis.
   ========================================================================== */
(function (global) {
  "use strict";

  // SVG stroke icon set (no emoji characters).
  const ICONS = {
    home: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 11l9-8 9 8"/><path d="M5 10v10h14V10"/></svg>',
    chat: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H8l-4 4V5a2 2 0 0 1 2-2h13a2 2 0 0 1 2 2z"/></svg>',
    memory: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 3h6a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z"/><path d="M9 7h6M9 11h6M9 15h4"/></svg>',
    music: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>',
    pet: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="4" r="2"/><circle cx="18" cy="8" r="2"/><circle cx="4" cy="8" r="2"/><circle cx="6" cy="16" r="2"/><circle cx="16" cy="16" r="2"/><path d="M8 14v4a2 2 0 0 0 2 2h0a4 4 0 0 0 4 0h0a2 2 0 0 0 2-2v-4"/></svg>',
    moments: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3a15 15 0 0 1 0 18M12 3a15 15 0 0 0 0 18"/></svg>',
    date: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>',
    settings: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-1.8-.3 1.6 1.6 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.6 1.6 0 0 0-1-1.5 1.6 1.6 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.6 1.6 0 0 0 .3-1.8 1.6 1.6 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.6 1.6 0 0 0 1.5-1 1.6 1.6 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.6 1.6 0 0 0 1.8.3H9a1.6 1.6 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.6 1.6 0 0 0 1 1.5 1.6 1.6 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0-.3 1.8V9a1.6 1.6 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.6 1.6 0 0 0-1.5 1z"/></svg>',
    brand: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 10.5L12 3l9 7.5"/><path d="M5 9.5V20h14V9.5"/><path d="M10 20v-5h4v5"/></svg>',
    send: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 2L11 13"/><path d="M22 2l-7 20-4-9-9-4z"/></svg>',
    spark: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3v4M12 17v4M3 12h4M17 12h4M6 6l2.5 2.5M15.5 15.5L18 18M18 6l-2.5 2.5M8.5 15.5L6 18"/></svg>',
    play: '<svg viewBox="0 0 24 24" fill="currentColor" stroke="none"><path d="M8 5v14l11-7z"/></svg>',
    pause: '<svg viewBox="0 0 24 24" fill="currentColor" stroke="none"><path d="M6 4h4v16H6zM14 4h4v16h-4z"/></svg>',
    heart: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8l1.1 1L12 21l7.7-7.6 1.1-1a5.5 5.5 0 0 0 0-7.8z"/></svg>',
    plus: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14M5 12h14"/></svg>',
    back: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>',
    search: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>',
    trash: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M8 6V4a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/></svg>',
    edit: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/></svg>',
    logout: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="M16 17l5-5-5-5M21 12H9"/></svg>',
    moon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/></svg>',
  };

  // Route -> page file (relative to /frontend/)
  const ROUTES = {
    home: "pages/home/index.html",
    chat: "pages/chat/index.html",
    memory: "pages/memory/index.html",
    music: "pages/music/index.html",
    pet: "pages/pet/index.html",
    moments: "pages/moments/index.html",
    date: "pages/date/index.html",
    settings: "pages/settings/index.html",
  };
  const BASE = "/frontend/";

  function icon(name) { return ICONS[name] || ""; }

  // ---- Global state ----
  const state = {
    route: "home",
    user: null,
    activeModel: null,
  };

  // ---- Theme ----
  function getTheme() {
    try { return localStorage.getItem("lv_theme") || "dark"; } catch (e) { return "dark"; }
  }
  function applyTheme(theme) {
    document.documentElement.setAttribute("data-theme", theme);
    try { localStorage.setItem("lv_theme", theme); } catch (e) {}
    const sw = document.getElementById("theme-switch");
    if (sw) sw.checked = theme === "light";
  }
  function toggleTheme() { applyTheme(getTheme() === "dark" ? "light" : "dark"); }

  // ---- Navigation ----
  function hrefFor(route) { return BASE + (ROUTES[route] || ROUTES.home); }
  function navigate(route) { global.location.href = hrefFor(route); }

  // Simple hash router: a hash like #/chat on any page redirects to that page.
  function onHashChange() {
    const raw = (global.location.hash || "").replace(/^#\/?/, "");
    if (!raw) return;
    const [route] = raw.split("/");
    if (ROUTES[route] && route !== currentPage()) navigate(route);
  }
  function currentPage() {
    return document.body.getAttribute("data-page") || "home";
  }

  // ---- Shell builder ----
  function navItem(route, label, iconKey) {
    return '<a class="nav-link" href="' + hrefFor(route) + '" data-nav="' + route + '">' +
      icon(iconKey) + "<span>" + label + "</span></a>";
  }
  function bottomNavItem(route, label, iconKey) {
    return '<a class="bottom-nav__link" href="' + hrefFor(route) + '" data-nav="' + route +
      '" aria-label="' + label + '">' + icon(iconKey) + "<span>" + label + "</span></a>";
  }

  function buildShell() {
    const root = document.getElementById("app");
    if (!root) return;
    if (root.getAttribute("data-built")) { mountContent(); return; }

    const desktopNav = [
      navItem("home", "首页", "home"),
      navItem("chat", "对话", "chat"),
      navItem("memory", "记忆", "memory"),
      navItem("music", "音乐", "music"),
      navItem("pet", "AI 宠物", "pet"),
      navItem("moments", "朋友圈", "moments"),
      navItem("date", "约会", "date"),
      navItem("settings", "设置", "settings"),
    ].join("");

    const mobileNav = [
      bottomNavItem("home", "首页", "home"),
      bottomNavItem("chat", "对话", "chat"),
      bottomNavItem("memory", "记忆", "memory"),
      bottomNavItem("music", "音乐", "music"),
      bottomNavItem("pet", "宠物", "pet"),
      bottomNavItem("settings", "设置", "settings"),
    ].join("");

    root.innerHTML =
      '<div class="app-layout">' +
        '<aside class="sidebar" aria-label="Primary navigation">' +
          '<a class="sidebar__brand" href="' + hrefFor("home") + '">' +
            '<span class="sidebar__logo">' + icon("brand") + "</span>" +
            "<div><div class=\"sidebar__name\">LandricsVilla</div>" +
            '<div class="sidebar__tag">local-first AI companion</div></div>' +
          "</a>" +
          desktopNav +
          '<div class="divider"></div>' +
          '<button class="nav-link" id="lv-theme-toggle" type="button" aria-label="切换主题">' +
          icon("moon") + "<span>切换主题</span></button>" +
        "</aside>" +
        '<main class="app-content" id="app-view" tabindex="-1"></main>' +
        '<nav class="bottom-nav" aria-label="Mobile navigation"><ul class="bottom-nav__list">' +
          mobileNav + "</ul></nav>" +
      "</div>";
    root.setAttribute("data-built", "1");

    const toggle = document.getElementById("lv-theme-toggle");
    if (toggle) toggle.addEventListener("click", toggleTheme);

    mountContent();
  }

  // Clone the page's <template id="page-content"> into the view, then run page init.
  function mountContent() {
    const view = document.getElementById("app-view");
    const page = currentPage();
    state.route = page;
    highlightNav(page);
    const tpl = document.getElementById("page-content");
    if (tpl && view && !view.getAttribute("data-mounted")) {
      view.appendChild(document.importNode(tpl.content, true));
      view.setAttribute("data-mounted", "1");
    }
    const initFn = global.LVPages && global.LVPages[page];
    if (typeof initFn === "function") {
      try { initFn(state, page); } catch (e) { console.error("page init error", e); }
    }
    view.scrollTop = 0;
  }

  function highlightNav(route) {
    document.querySelectorAll("[data-nav]").forEach(function (el) {
      el.classList.toggle("is-active", el.getAttribute("data-nav") === route);
    });
  }

  // ---- Utilities exposed globally ----
  function escapeHtml(s) {
    return String(s == null ? "" : s)
      .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
  }
  function el(html) {
    const t = document.createElement("template");
    t.innerHTML = html.trim();
    return t.content.firstElementChild;
  }
  function toast(msg, kind) {
    let box = document.getElementById("lv-toast");
    if (!box) {
      box = document.createElement("div");
      box.id = "lv-toast";
      box.setAttribute("role", "status");
      box.setAttribute("aria-live", "polite");
      box.style.cssText =
        "position:fixed;bottom:80px;left:50%;transform:translateX(-50%);" +
        "background:var(--surface);color:var(--text);padding:10px 16px;border-radius:12px;" +
        "border:1px solid var(--border);box-shadow:var(--shadow);z-index:200;font-size:14px;max-width:90vw";
      document.body.appendChild(box);
    }
    box.textContent = msg;
    box.style.color = kind === "error" ? "var(--danger)" : (kind === "ok" ? "var(--success)" : "var(--text)");
    clearTimeout(box._t);
    box._t = setTimeout(function () { box.textContent = ""; }, 2600);
  }
  // Lightweight, safe markdown-ish renderer (no external deps). Escapes first.
  function renderMarkdown(text) {
    let s = escapeHtml(text);
    // code blocks
    s = s.replace(/```([\s\S]*?)```/g, function (_, c) {
      return '<pre class="md-pre"><code>' + c + "</code></pre>";
    });
    // inline code
    s = s.replace(/`([^`]+)`/g, '<code class="md-code">$1</code>');
    // bold
    s = s.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>");
    s = s.replace(/__([^_]+)__/g, "<strong>$1</strong>");
    // italics
    s = s.replace(/\*([^*]+)\*/g, "<em>$1</em>");
    // links
    s = s.replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>');
    // line breaks
    s = s.replace(/\n/g, "<br>");
    return s;
  }

  // ---- Init ----
  function init() {
    applyTheme(getTheme());
    state.user = global.LVApi ? global.LVApi.getUser() : null;
    try { state.activeModel = localStorage.getItem("lv_active_model") || null; } catch (e) {}
    buildShell();
    global.addEventListener("hashchange", onHashChange);
  }

  global.LVApp = {
    init: init, navigate: navigate, hrefFor: hrefFor,
    state: state, icon: icon, ROUTES: ROUTES,
    escapeHtml: escapeHtml, el: el, toast: toast, renderMarkdown: renderMarkdown,
    getTheme: getTheme, applyTheme: applyTheme, toggleTheme: toggleTheme,
  };
  global.LVPages = global.LVPages || {};
})(window);
