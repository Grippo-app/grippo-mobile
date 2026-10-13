# Annotation binding patterns

```kotlin
@Single(binds = [UserFeature::class])
internal class UserFeatureImpl(private val repository: UserRepository) : UserFeature
```

This illustrates visibility/binding only; use the existing implementation rather than copying a
partial class. Public Feature interface, internal implementation and public generated module form
separate responsibilities. KSP wiring follows `build-logic/convention/.../KoinAnnotationConventionPlugin.kt`.

- `@Single` fits app-lifetime stateless services/shared owners; `@Factory` fits per-instance state
  where the existing consumer supplies its scope. Read lifecycle/disposal before choosing.
- Interface-backed implementations need explicit binds; concrete providers resolve under their
  intended type. Multiple implementations need intentional qualifiers and matching consumer lookup.
- Provider methods handle platform factory/builder adapters. Do not add ComponentScan merely
  because another module has it. Match the owning module's annotated pattern.
- FeatureApiModule's existing module DSL registers plain UseCases. Extending it for an authorized
  UseCase is permitted; a new DSL module is not.
- Do not change KSP configuration checks or convention-plugin wiring as a feature fix.
