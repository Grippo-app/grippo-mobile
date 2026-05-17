(function () {
  window.App = window.App || {};

  // Supported placeholder tokens (kept here as a single reference table so
  // future maintainers can extend the substitution map without hunting
  // through panel code):
  //
  //   "com.<org>.<product>.android" → setup.applicationId
  //   "com.<org>.<product>"         → derived productPackage
  //                                   (com.<org>.<product>, or com.<product>
  //                                    when orgName is empty)
  //   "<product-domain>"            → setup.backendHost
  //   "<Product>Api"                → setup.productName + "Api"
  //   "<Product>"                   → setup.productName
  //   "<product>"                   → setup.productName.toLowerCase()
  //   "<org>"                       → setup.orgName (may be empty)
  //   "<typeface>"                  → setup.typefaceFactory
  //   "<firstDomain>"               → setup.firstDomain
  //   "<iosFrameworkName>"          → setup.iosFrameworkName
  //   "<IosFrameworkName>"          → PascalCase(iosFrameworkName) — first
  //                                   letter upper-cased; used in the Gradle
  //                                   task `assemble<IosFrameworkName>DebugXCFramework`
  //
  // Substitution is performed in a single pass via a regex split so an
  // earlier replacement cannot create text that later matches a different
  // placeholder.

  function safeStr(v) {
    return v == null ? '' : String(v);
  }

  function pascalFirst(v) {
    var s = safeStr(v);
    if (s.length === 0) return '';
    return s.charAt(0).toUpperCase() + s.slice(1);
  }

  function derivedProductPackage(setup) {
    var product = safeStr(setup && setup.productName).toLowerCase();
    var org = safeStr(setup && setup.orgName);
    if (org.length > 0) {
      return 'com.' + org + '.' + product;
    }
    return 'com.' + product;
  }

  // Token table, ORDERED LONGEST-FIRST so that a shorter token never
  // matches text that is part of a longer token. The first entry that
  // matches a slice wins.
  function buildTable(setup) {
    var product = safeStr(setup && setup.productName);
    var productLower = product.toLowerCase();
    var pkg = derivedProductPackage(setup);
    return [
      { token: 'com.<org>.<product>.android', value: safeStr(setup && setup.applicationId) },
      { token: 'com.<org>.<product>',         value: pkg },
      { token: '<product-domain>',            value: safeStr(setup && setup.backendHost) },
      { token: '<Product>Api',                value: product + 'Api' },
      { token: '<Product>',                   value: product },
      { token: '<product>',                   value: productLower },
      { token: '<org>',                       value: safeStr(setup && setup.orgName) },
      { token: '<typeface>',                  value: safeStr(setup && setup.typefaceFactory) },
      { token: '<firstDomain>',               value: safeStr(setup && setup.firstDomain) },
      { token: '<IosFrameworkName>',          value: pascalFirst(setup && setup.iosFrameworkName) },
      { token: '<iosFrameworkName>',          value: safeStr(setup && setup.iosFrameworkName) }
    ];
  }

  function escapeRegex(s) {
    return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }

  function render(template, setup) {
    if (template == null) return '';
    var src = String(template);
    var table = buildTable(setup || {});
    var pattern = table.map(function (e) { return escapeRegex(e.token); }).join('|');
    var re = new RegExp(pattern, 'g');
    // Split the source on the union pattern, then walk the pieces. The
    // regex .split with a capturing group would also work, but a manual
    // walk via .replace is simpler — each match resolves through the
    // table's first-matching entry, which by construction (longest
    // tokens first in the alternation) prefers the longest match.
    return src.replace(re, function (match) {
      for (var i = 0; i < table.length; i++) {
        if (table[i].token === match) return table[i].value;
      }
      return match;
    });
  }

  window.App.templates = {
    render: render
  };
})();
