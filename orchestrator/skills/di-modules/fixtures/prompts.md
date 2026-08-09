# di-modules — trigger fixtures

Realistic prompts that SHOULD activate the `di-modules` skill (Koin Annotations + KSP wiring).

- add a Koin module for the profile feature
- provide a @Single binding with binds = [ProfileRepository::class]
- wire a viewModel factory binding for the new ViewModel
- register the new module in :shared/Koin.kt modules(...)
- compose the modules with @Module(includes = [...])
- add an inline @Single internal fun provider for the http client
