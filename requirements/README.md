# System Requirements — KMP Mobile Project

Architecture and conventions for bootstrapping a new Kotlin Multiplatform mobile project (Android + iOS, Compose Multiplatform, Decompose, Koin, Ktor, Room). Derived from the `grippo-mobile` reference project; written to be product-agnostic.

Everything is driven from a static site — Setup, Launch Wizard, sub-agent install, task generator. Open it and follow the steps.

## Run the site

From this folder:

```bash
cd site && python3 -m http.server 8000
```

Then open <http://localhost:8000>. Ctrl-C to stop.

Prefer raw markdown? Each numbered folder (`00-overview/` … `14-cookbook/`) is normative documentation; `launch.md` is the same wizard in long form.
