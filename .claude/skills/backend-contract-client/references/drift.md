# Mobile/backend drift review

Inspect GrippoApi method/path/query/body/result, DTO SerialNames and types, dictionary promotion,
required mapper guards and the complete Repository/UseCase consumers.

Report each substantive finding with authoritative evidence/revision, actual mobile file/line,
affected consumers, behavioral impact and a proposed correction. Distinguish a removed operation,
wrong wire type/key, unknown dictionary value and missing optional data; they need different repairs.
Do not equate backend non-null declarations with a need to make mobile DTO properties non-null.

A source audit can establish code mismatch but cannot prove deployed runtime compatibility.
An example payload does not prove a closed set. Missing contract evidence stays explicitly unresolved.
When authorized to fix, update the coherent consumer chain without silently changing unrelated
public contracts, dependencies or persisted schemas. No automatic codegen or speculative fallback values.
