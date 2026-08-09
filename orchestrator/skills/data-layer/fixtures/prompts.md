# data-layer — trigger fixtures

Realistic prompts that SHOULD activate the `data-layer` skill (feature-api / repository / Room / DataStore).

- add a new :data-features:profile module exposing a ProfileFeature
- add a repository and feature-api split for the new domain concept
- add a Room entity, DAO, and write the migration for the new column
- persist a key-value preference via DataStore on the owning repository
- add a domain model and enum for the closed exercise-category set
- extend the <Product>Api with a new endpoint and its DTO
