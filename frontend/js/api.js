/* ==========================================================================
   LandricsVilla - API client
   Centralized fetch wrapper with auth header + all endpoint functions.
   No hardcoded keys anywhere. Users supply their own API key on Settings.
   ========================================================================== */
(function (global) {
  "use strict";

  // Backend base URL. Override at runtime via window.LV_BASE_URL if needed.
  const BASE_URL = (global.LV_BASE_URL || "http://localhost:8000").replace(/\/$/, "");

  const TOKEN_KEY = "lv_token";
  const USER_KEY = "lv_user";

  function getToken() {
    try { return localStorage.getItem(TOKEN_KEY) || ""; } catch (e) { return ""; }
  }
  function setToken(t) {
    try { t ? localStorage.setItem(TOKEN_KEY, t) : localStorage.removeItem(TOKEN_KEY); } catch (e) {}
  }
  function getUser() {
    try { return JSON.parse(localStorage.getItem(USER_KEY) || "null"); } catch (e) { return null; }
  }
  function setUser(u) {
    try { u ? localStorage.setItem(USER_KEY, JSON.stringify(u)) : localStorage.removeItem(USER_KEY); } catch (e) {}
  }

  // Core request wrapper
  async function request(path, opts) {
    opts = opts || {};
    const headers = Object.assign({}, opts.headers || {});
    if (!(global.FormData && opts.body instanceof global.FormData)) {
      headers["Content-Type"] = headers["Content-Type"] || "application/json";
    }
    const token = getToken();
    if (token) headers["Authorization"] = "Bearer " + token;

    let body = opts.body;
    if (body && headers["Content-Type"] === "application/json" && typeof body !== "string") {
      body = JSON.stringify(body);
    }

    let res;
    try {
      res = await fetch(BASE_URL + path, {
        method: opts.method || "GET",
        headers: headers,
        body: body,
        credentials: opts.credentials || "omit",
      });
    } catch (e) {
      throw new ApiError("Network error: " + (e && e.message ? e.message : "unreachable"), 0, e);
    }

    const isJson = (res.headers.get("content-type") || "").includes("application/json");
    const payload = isJson ? await res.json().catch(() => null) : await res.text().catch(() => "");

    if (!res.ok) {
      const msg = (payload && (payload.detail || payload.message || payload.error)) ||
        (typeof payload === "string" && payload) ||
        ("Request failed (" + res.status + ")");
      throw new ApiError(msg, res.status, payload);
    }
    return payload;
  }

  function ApiError(message, status, data) {
    const err = new Error(message);
    err.name = "ApiError";
    err.status = status;
    err.data = data;
    return err;
  }

  // ---- Auth ----
  const Auth = {
    register: (data) => request("/api/user/register", { method: "POST", body: data }),
    login: (data) => request("/api/user/login", { method: "POST", body: data }),
    me: () => request("/api/user/me"),
    updateMe: (data) => request("/api/user/me", { method: "PUT", body: data }),
    logout: () => { setToken(""); setUser(null); },
  };

  // ---- Models ----
  // User supplies provider + apiKey + baseUrl; backend fetches models list.
  const Models = {
    fetchList: function (provider, apiKey, baseUrl) {
      return request("/api/models/list", {
        method: "POST",
        body: { provider: provider, api_key: apiKey, base_url: baseUrl },
      });
    },
    // Convenience returning an array of model id strings.
    fetchModels: async function (provider, apiKey, baseUrl) {
      const res = await this.fetchList(provider, apiKey, baseUrl);
      const list = (res && res.models) || [];
      return list.map(function (m) {
        return typeof m === "string" ? m : (m.id || m.name || m.model || JSON.stringify(m));
      });
    },
    test: (data) => request("/api/models/test", { method: "POST", body: data }),
    saveConfig: (data) => request("/api/models/config", { method: "PUT", body: data }),
  };

  // ---- Chat ----
  const Chat = {
    send: (data) => request("/api/chat/send", { method: "POST", body: data }),
    history: (conversationId) => request("/api/chat/history/" + encodeURIComponent(conversationId)),
  };

  // ---- Memory ----
  const Memory = {
    list: (params) => request("/api/memory" + toQuery(params)),
    get: (id) => request("/api/memory/" + encodeURIComponent(id)),
    create: (data) => request("/api/memory", { method: "POST", body: data }),
    update: (id, data) => request("/api/memory/" + encodeURIComponent(id), { method: "PUT", body: data }),
    remove: (id) => request("/api/memory/" + encodeURIComponent(id), { method: "DELETE" }),
  };

  // ---- Music ----
  const Music = {
    generate: (data) => request("/api/music/generate", { method: "POST", body: data }),
    list: (params) => request("/api/music/list" + toQuery(params)),
    audioUrl: (id) => BASE_URL + "/api/music/" + encodeURIComponent(id) + "/audio",
  };

  // ---- Pet ----
  const Pet = {
    get: () => request("/api/pet"),
    update: (data) => request("/api/pet", { method: "PUT", body: data }),
    interact: (data) => request("/api/pet", { method: "POST", body: data }),
  };

  // ---- Moments ----
  const Moments = {
    feed: (params) => request("/api/moments/feed" + toQuery(params)),
    create: (data) => request("/api/moments/create", { method: "POST", body: data }),
    like: (id) => request("/api/moments/" + encodeURIComponent(id) + "/like", { method: "POST" }),
  };

  // ---- MCP ----
  const Mcp = {
    tools: () => request("/api/mcp/tools"),
    call: (data) => request("/api/mcp/call", { method: "POST", body: data }),
  };

  function toQuery(params) {
    if (!params) return "";
    const parts = [];
    Object.keys(params).forEach(function (k) {
      const v = params[k];
      if (v === undefined || v === null || v === "") return;
      parts.push(encodeURIComponent(k) + "=" + encodeURIComponent(String(v)));
    });
    return parts.length ? "?" + parts.join("&") : "";
  }

  const Api = {
    BASE_URL: BASE_URL,
    getToken: getToken, setToken: setToken,
    getUser: getUser, setUser: setUser,
    request: request,
    Auth: Auth, Models: Models, Chat: Chat, Memory: Memory,
    Music: Music, Pet: Pet, Moments: Moments, Mcp: Mcp,
  };
  global.LVApi = Api;
})(window);
