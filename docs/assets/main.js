/* NotifyHub site behaviour: code highlighter, copy buttons, burger menu, docs sections. */
(function () {
  "use strict";
  document.documentElement.classList.add("js");

  /* ---------- Syntax highlighter ---------- */
  var KW_JS = "const|let|var|function|return|if|else|for|while|await|async|new|import|from|export|class|try|catch|throw|true|false|null|undefined";
  var LANGS = {
    bash: [
      ["c", /#[^\n]*/],
      ["s", /"(?:\\.|[^"\\])*"|'[^']*'/],
      ["o", /(?<=\s)--?[A-Za-z][\w-]*/],
      ["p", /\$\{?[A-Za-z_]\w*\}?/],
      ["k", /\b(?:if|then|else|fi|for|do|done|export|echo|cd)\b/],
      ["f", /\b(?:curl|git|gradle|gradlew|adb|python3?|npm|node|cp|mkdir|ls|cat|chmod|sudo)\b/],
      ["n", /\b\d+\b/],
      ["x", /[\\|&;<>()]/]
    ],
    json: [
      ["p", /"(?:\\.|[^"\\])*"(?=\s*:)/],
      ["s", /"(?:\\.|[^"\\])*"/],
      ["n", /-?\b\d+(?:\.\d+)?\b/],
      ["k", /\b(?:true|false|null)\b/],
      ["x", /[{}\[\],:]/]
    ],
    js: [
      ["c", /\/\/[^\n]*|\/\*[\s\S]*?\*\//],
      ["s", /"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|`(?:\\.|[^`\\])*`/],
      ["n", /\b\d+(?:\.\d+)?\b/],
      ["k", new RegExp("\\b(?:" + KW_JS + ")\\b")],
      ["f", /\b[A-Za-z_$][\w$]*(?=\()/],
      ["x", /[{}()\[\];,.:=<>+\-*\/!?&|]/]
    ],
    py: [
      ["c", /#[^\n]*/],
      ["s", /"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'/],
      ["n", /\b\d+(?:\.\d+)?\b/],
      ["k", /\b(?:def|class|return|if|elif|else|for|while|import|from|as|with|try|except|raise|True|False|None|in|not|and|or|lambda|async|await)\b/],
      ["f", /\b[A-Za-z_]\w*(?=\()/],
      ["x", /[{}()\[\],.:=<>+\-*\/!]/]
    ],
    yaml: [
      ["c", /#[^\n]*/],
      ["s", /"(?:\\.|[^"\\])*"|'[^']*'/],
      ["p", /[\w.-]+(?=\s*:)/],
      ["n", /\b\d+(?:\.\d+)?\b/],
      ["k", /\b(?:true|false|null)\b/],
      ["x", /[:\-\[\]{},|>]/]
    ],
    kotlin: [
      ["c", /\/\/[^\n]*|\/\*[\s\S]*?\*\//],
      ["s", /"(?:\\.|[^"\\])*"/],
      ["n", /\b\d+(?:\.\d+)?[Ll]?\b/],
      ["t", /@\w+/],
      ["k", /\b(?:package|import|class|object|interface|fun|val|var|return|if|else|when|for|while|in|is|as|null|true|false|suspend|override|private|data|companion|const)\b/],
      ["f", /\b[A-Za-z_]\w*(?=\()/],
      ["t", /\b[A-Z]\w*\b/],
      ["x", /[{}()\[\],.:=<>+\-*\/!?]/]
    ],
    http: [
      ["k", /^(?:GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\b/m],
      ["t", /\bHTTP\/\d(?:\.\d)?/],
      ["p", /^[A-Za-z-]+(?=:)/m],
      ["n", /\b\d{3}\b/],
      ["s", /\/[^\s?]*/],
      ["x", /[?&=:]/]
    ],
    text: []
  };
  var ALIAS = { sh: "bash", shell: "bash", javascript: "js", python: "py", kt: "kotlin", yml: "yaml", plain: "text", txt: "text" };

  function esc(s) { return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;"); }

  function highlight(src, lang) {
    var rules = LANGS[ALIAS[lang] || lang];
    if (!rules || !rules.length) return esc(src);
    var parts = [], flagsOf = [];
    rules.forEach(function (r) {
      parts.push("(" + r[1].source + ")");
      flagsOf.push(r[1].flags.indexOf("m") > -1);
    });
    var multi = flagsOf.some(Boolean);
    var re = new RegExp(parts.join("|"), "g" + (multi ? "m" : ""));
    var out = "", last = 0, m;
    while ((m = re.exec(src)) !== null) {
      if (m[0] === "") { re.lastIndex++; continue; }
      var idx = -1;
      for (var i = 1; i <= rules.length; i++) { if (m[i] !== undefined) { idx = i - 1; break; } }
      out += esc(src.slice(last, m.index));
      out += idx < 0 ? esc(m[0]) : '<span class="tok-' + rules[idx][0] + '">' + esc(m[0]) + "</span>";
      last = m.index + m[0].length;
    }
    return out + esc(src.slice(last));
  }

  function copyText(text) {
    if (navigator.clipboard && window.isSecureContext) return navigator.clipboard.writeText(text);
    return new Promise(function (resolve, reject) {
      var ta = document.createElement("textarea");
      ta.value = text; ta.setAttribute("readonly", ""); ta.style.cssText = "position:fixed;opacity:0;top:0";
      document.body.appendChild(ta); ta.select();
      try { document.execCommand("copy") ? resolve() : reject(); } catch (e) { reject(e); }
      document.body.removeChild(ta);
    });
  }

  function enhanceCode() {
    document.querySelectorAll("pre > code[data-lang]").forEach(function (code) {
      var pre = code.parentNode;
      var raw = code.textContent.replace(/^\n+|\s+$/g, "");
      var lang = code.getAttribute("data-lang");
      var title = code.getAttribute("data-title") || lang;
      try { code.innerHTML = highlight(raw, lang); } catch (e) { code.textContent = raw; }

      var wrap = document.createElement("div");
      wrap.className = "code";
      var bar = document.createElement("div");
      bar.className = "code-bar";
      bar.innerHTML = '<span class="dots" aria-hidden="true"><i></i><i></i><i></i></span><span class="code-title"></span>' +
        '<button type="button" class="copy" aria-label="Copy code"><svg class="icon" aria-hidden="true"><use href="#i-copy"/></svg><span>Copy</span></button>';
      bar.querySelector(".code-title").textContent = title;
      var btn = bar.querySelector(".copy");
      btn.addEventListener("click", function () {
        copyText(raw).then(function () {
          btn.classList.add("done");
          btn.querySelector("span").textContent = "Copied";
          btn.querySelector("use").setAttribute("href", "#i-check");
          setTimeout(function () {
            btn.classList.remove("done");
            btn.querySelector("span").textContent = "Copy";
            btn.querySelector("use").setAttribute("href", "#i-copy");
          }, 1800);
        }, function () {});
      });
      pre.className = "code-body";
      pre.tabIndex = 0;
      pre.parentNode.insertBefore(wrap, pre);
      wrap.appendChild(bar);
      wrap.appendChild(pre);
    });
  }

  /* ---------- Burger menu ---------- */
  var root = document.documentElement;
  var burger, BREAKPOINT = 820;
  function setMenu(open) {
    if (!burger) return;
    root.classList.toggle("menu-open", open);
    burger.setAttribute("aria-expanded", open ? "true" : "false");
    burger.setAttribute("aria-label", open ? "Close menu" : "Open menu");
  }
  function closeMenu() { setMenu(false); }
  window.NH_closeMenu = closeMenu;

  function initMenu() {
    burger = document.querySelector(".burger");
    if (!burger) return;
    burger.addEventListener("click", function () { setMenu(!root.classList.contains("menu-open")); });
    var menu = document.getElementById(burger.getAttribute("aria-controls"));
    if (menu) menu.addEventListener("click", function (e) { if (e.target.closest("a")) closeMenu(); });
    document.addEventListener("keydown", function (e) { if (e.key === "Escape") closeMenu(); });
    window.addEventListener("resize", function () { if (window.innerWidth > BREAKPOINT) closeMenu(); });
  }

  /* ---------- Docs sections ---------- */
  function initDocs() {
    var sections = Array.prototype.slice.call(document.querySelectorAll(".doc-section"));
    if (!sections.length) return;
    var links = document.querySelectorAll("[data-section]");
    var siteName = document.title.split(" | ").pop();
    var ids = sections.map(function (s) { return s.id; });

    function show(id, push) {
      if (ids.indexOf(id) < 0) id = ids[0];
      sections.forEach(function (s) { s.classList.toggle("active", s.id === id); });
      links.forEach(function (a) {
        if (a.getAttribute("data-section") === id) a.setAttribute("aria-current", "true");
        else a.removeAttribute("aria-current");
      });
      var h1 = document.querySelector("#" + id + " h1");
      document.title = (h1 ? h1.textContent : id) + " | " + siteName;
      if (push) { try { history.pushState(null, "", "#" + id); } catch (e) {} }
      window.scrollTo(0, 0);
      if (window.NH_closeMenu) window.NH_closeMenu();
    }

    document.addEventListener("click", function (e) {
      var a = e.target.closest("[data-section], [data-goto]");
      if (!a) return;
      var id = a.getAttribute("data-section") || a.getAttribute("data-goto");
      e.preventDefault();
      show(id, true);
    });
    window.addEventListener("popstate", function () { show(location.hash.slice(1), false); });
    show(location.hash.slice(1), false);
    setTimeout(function () { window.scrollTo(0, 0); }, 0);
  }

  document.addEventListener("DOMContentLoaded", function () {
    enhanceCode();
    initMenu();
    initDocs();
  });
})();
