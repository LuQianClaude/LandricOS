/* ==========================================================================
   LandricsVilla - WebSocket manager for chat streaming
   Handles connect / reconnect / token dispatch / graceful teardown.
   Emits tokens via callback. Auto-reconnect with backoff.
   ========================================================================== */
(function (global) {
  "use strict";

  function buildWsUrl(conversationId) {
    // ws(s) scheme mirrors http(s) of the API base URL.
    const base = (global.LVApi && global.LVApi.BASE_URL) || "http://localhost:8000";
    const wsBase = base.replace(/^http/, "ws").replace(/\/$/, "");
    return wsBase + "/api/chat/ws/chat/" + encodeURIComponent(conversationId);
  }

  function ChatSocket(conversationId, handlers) {
    this.conversationId = conversationId;
    this.handlers = handlers || {};
    this.ws = null;
    this.closed = false;          // user-intentional close
    this.reconnectAttempts = 0;
    this.maxAttempts = 5;
    this.queue = [];              // messages buffered before open
    this.url = buildWsUrl(conversationId);
    // attach token as query param so ws auth works (some servers need it)
    const token = global.LVApi && global.LVApi.getToken();
    if (token) this.url += "?token=" + encodeURIComponent(token);
  }

  ChatSocket.prototype.connect = function () {
    const self = this;
    if (!global.WebSocket) {
      this._fail(new Error("WebSocket not supported in this browser"));
      return;
    }
    try {
      this.ws = new WebSocket(this.url);
    } catch (e) {
      this._fail(e);
      return;
    }
    this.ws.onopen = function () {
      self.reconnectAttempts = 0;
      // flush queued messages
      while (self.queue.length) { self._rawSend(self.queue.shift()); }
      if (typeof self.handlers.onOpen === "function") self.handlers.onOpen();
    };
    this.ws.onmessage = function (evt) { self._dispatch(evt); };
    this.ws.onerror = function () {
      if (typeof self.handlers.onError === "function")
        self.handlers.onError(new Error("WebSocket error"));
    };
    this.ws.onclose = function (evt) {
      if (self.closed) {
        if (typeof self.handlers.onClose === "function") self.handlers.onClose(evt);
        return;
      }
      // unexpected close -> attempt reconnect
      if (typeof self.handlers.onClose === "function") self.handlers.onClose(evt);
      self._scheduleReconnect();
    };
  };

  ChatSocket.prototype._rawSend = function (data) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(typeof data === "string" ? data : JSON.stringify(data));
    }
  };

  ChatSocket.prototype.send = function (payload) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this._rawSend(payload);
    } else {
      this.queue.push(payload); // buffer until connected
    }
  };

  // Dispatch incoming frames. Supports both raw text tokens and JSON envelopes.
  ChatSocket.prototype._dispatch = function (evt) {
    const data = evt.data;
    const h = this.handlers;
    // Blob handling (binary frames): convert to text first.
    if (data instanceof Blob) {
      const self = this;
      data.text().then(function (txt) { self._handleText(txt, h); });
      return;
    }
    if (typeof data === "string") { this._handleText(data, h); }
  };

  ChatSocket.prototype._handleText = function (text, h) {
    let parsed = null;
    try { parsed = JSON.parse(text); } catch (e) { parsed = null; }
    if (parsed) {
      if (parsed.type === "token" || parsed.type === "delta") {
        if (typeof h.onToken === "function") h.onToken(parsed.content || parsed.text || parsed.delta || "");
      } else if (parsed.type === "done" || parsed.type === "end") {
        if (typeof h.onDone === "function") h.onDone(parsed);
      } else if (parsed.type === "error") {
        if (typeof h.onError === "function") h.onError(new Error(parsed.message || parsed.error || "stream error"));
      } else if (parsed.type === "start" || parsed.type === "meta") {
        if (typeof h.onMeta === "function") h.onMeta(parsed);
      } else {
        // generic envelope with content field
        if (parsed.content && typeof h.onToken === "function") h.onToken(parsed.content);
        else if (typeof h.onMessage === "function") h.onMessage(parsed);
      }
    } else {
      // plain text token
      if (typeof h.onToken === "function") h.onToken(text);
    }
  };

  ChatSocket.prototype._scheduleReconnect = function () {
    if (this.reconnectAttempts >= this.maxAttempts) {
      this._fail(new Error("Connection lost after " + this.maxAttempts + " attempts"));
      return;
    }
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 15000);
    this.reconnectAttempts++;
    if (typeof this.handlers.onReconnect === "function")
      this.handlers.onReconnect(this.reconnectAttempts, delay);
    const self = this;
    this._timer = setTimeout(function () { self.connect(); }, delay);
  };

  ChatSocket.prototype._fail = function (err) {
    if (typeof this.handlers.onError === "function") this.handlers.onError(err);
    if (typeof this.handlers.onDone === "function") this.handlers.onDone({ error: err.message });
  };

  ChatSocket.prototype.close = function () {
    this.closed = true;
    if (this._timer) clearTimeout(this._timer);
    if (this.ws) {
      try { this.ws.close(); } catch (e) {}
      this.ws = null;
    }
  };

  global.LVWs = { ChatSocket: ChatSocket, buildWsUrl: buildWsUrl };
})(window);
