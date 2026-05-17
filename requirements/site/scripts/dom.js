(function () {
  window.App = window.App || {};

  // ----------------------------------------------------------------------
  // Shared DOM helper. Previously copy-pasted into every panel file —
  // consolidated here so the five panels reference one implementation.
  //
  // el(tag, attrs?, children?)
  //   attrs:
  //     class     -> node.className
  //     text      -> textContent
  //     html      -> innerHTML
  //     data: { k: v } -> setAttribute('data-<kebab(k)>', v) — camelCase
  //                       keys are converted to kebab-case so that the
  //                       lowercased attribute name HTML stores matches
  //                       [data-kebab-key] CSS/JS selectors callers use.
  //     attrs: { k: v } -> setAttribute(k, v)
  //     anything else -> direct property assignment (e.g. type, id, value)
  //   children: array of Node | string | null (nulls are skipped).
  // ----------------------------------------------------------------------

  // taskField -> task-field. Already-hyphenated and all-lowercase keys
  // pass through unchanged.
  function kebab(s) {
    return String(s).replace(/[A-Z]/g, function (c) { return '-' + c.toLowerCase(); });
  }

  function el(tag, attrs, children) {
    var node = document.createElement(tag);
    if (attrs) {
      var keys = Object.keys(attrs);
      for (var i = 0; i < keys.length; i++) {
        var k = keys[i];
        var v = attrs[k];
        if (k === 'class') node.className = v;
        else if (k === 'text') node.textContent = v;
        else if (k === 'html') node.innerHTML = v;
        else if (k === 'data') {
          var dk = Object.keys(v);
          for (var d = 0; d < dk.length; d++) node.setAttribute('data-' + kebab(dk[d]), v[dk[d]]);
        } else if (k === 'attrs') {
          var ak = Object.keys(v);
          for (var a = 0; a < ak.length; a++) node.setAttribute(ak[a], v[ak[a]]);
        } else {
          node[k] = v;
        }
      }
    }
    if (children) {
      for (var c = 0; c < children.length; c++) {
        var kid = children[c];
        if (kid == null) continue;
        if (typeof kid === 'string') node.appendChild(document.createTextNode(kid));
        else node.appendChild(kid);
      }
    }
    return node;
  }

  window.App.dom = { el: el };
})();
