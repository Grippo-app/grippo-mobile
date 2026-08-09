# backend-contract-client — trigger fixtures

Realistic prompts that SHOULD activate the `backend-contract-client` skill (OpenAPI/Postman snapshot + drift).

- check the backend contract before I add this DTO
- refresh the latest OpenAPI/Swagger snapshot — contract:refresh-openapi
- audit the data layer for contract drift against the committed snapshot
- refresh the snapshot from the Postman collection — contract:refresh-postman
- look up the endpoint inventory and the per-area field slice for profile
- the server changed an endpoint — did the client drift? run contract:diff
