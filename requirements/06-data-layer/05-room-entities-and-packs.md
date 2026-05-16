# Room Entities, DAOs, and `*Pack` Models

## Entities

```kotlin
@Entity(
    tableName = "note",
    indices = [Index(value = ["profileId"])],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["profileId"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
public data class NoteEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val title: String,
    val body: String,
    val createdAt: String,
    val updatedAt: String,
)
```

### Rules

1. **`@Entity public data class <X>Entity`** — always `data class`, always with the `Entity` suffix.
2. **`tableName = "snake_case_lower"`** — table names in snake_case for SQL friendliness.
3. **`@PrimaryKey val id: String`** — typically `String` (server-side UUID). Avoid auto-generated `Long` keys; the backend assigns IDs.
4. **Required fields are non-null; genuinely-optional fields stay nullable.** The DTO→Entity mapper validates required fields via `AppLogger.Mapping.log(value) { ... } ?: return null` (see `07-mappers/03-null-safety.md`) so the entity layer rejects partially-populated server rows. Fields that may be legitimately absent are declared `T?` — typical patterns: a one-of-N variant field where only one of a set of nullable columns is populated per row; an optional secondary attribute (`<Entity>.secondaryValue` / `<Entity>.lastConfirmedAt`); an optional foreign-key reference on a draft (`Draft<Entity>.<parent>Id` — `null` on a new draft, non-null when editing an existing parent); and the partial-snapshot fields on `TokenEntity` (`access` / `refresh`) that may be absent during refresh. Nullable entity fields do **not** carry a `= null` default — the mapper is required to make the choice explicitly; `TokenEntity` is the lone exception because its rows are written from inside `TokenProvider` rather than a DTO mapper.
5. **`@Index` for every column queried** (foreign keys, search columns, range columns). Skip indices only on columns never used in `WHERE` / `ORDER BY` / `JOIN`.
6. **`@ForeignKey` for parent-child relations** with `onDelete = ForeignKey.CASCADE` — when the parent is deleted, children are dropped. Prevents orphans.
7. **Timestamps are ISO-8601 UTC strings.** Stored as `String`, not `Long` (epoch ms). The string form is human-debuggable and trivially comparable lexicographically.
8. **Snake-case `@ColumnInfo(name = ...)` overrides** only when the column name must differ from the Kotlin name (rare). Default Room translates camelCase Kotlin to the same name in SQL.

### Composite primary keys

For join tables:

```kotlin
@Entity(
    tableName = "user_tag",
    primaryKeys = ["profileId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["profileId"], childColumns = ["profileId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [
        Index("profileId"),
        Index("tagId"),
    ],
)
public data class UserTagEntity(
    val profileId: String,
    val tagId: String,
)
```

`primaryKeys = ["a", "b"]` instead of `@PrimaryKey val ...` for composite PKs. Each FK column also gets a dedicated `@Index` — Room emits a build-time warning otherwise.

## DAOs

```kotlin
@Dao
public interface NoteDao {

    @Transaction
    @Query("""
        SELECT * FROM note
        WHERE createdAt BETWEEN :from AND :to
        ORDER BY createdAt DESC
    """)
    public fun get(from: String, to: String): Flow<List<NotePack>>

    @Transaction
    @Query("""
        SELECT * FROM note
        WHERE id = :id
        ORDER BY createdAt DESC
        LIMIT 1
    """)
    public fun getById(id: String): Flow<NotePack?>

    @Transaction
    public suspend fun insertOrReplace(
        note: NoteEntity,
        tags: List<TagEntity>,
        items: List<ItemEntity>,
    ) {
        insertNote(note)
        if (tags.isNotEmpty()) insertTags(tags)
        if (items.isNotEmpty()) insertItems(items)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertTags(tags: List<TagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertItems(items: List<ItemEntity>)

    @Query("DELETE FROM note")
    public suspend fun delete()

    @Query("DELETE FROM note WHERE id = :id")
    public suspend fun deleteById(id: String)

    @Query("DELETE FROM note WHERE createdAt BETWEEN :from AND :to")
    public suspend fun deleteByCreatedAtRange(from: String, to: String)

    @Query("""
        DELETE FROM note
        WHERE createdAt BETWEEN :from AND :to
        AND id NOT IN (:ids)
    """)
    public suspend fun deleteByCreatedAtRangeExceptIds(
        from: String,
        to: String,
        ids: List<String>,
    )
}
```

### Rules

1. **`@Dao public interface <X>Dao`** — always `interface`, never `abstract class` (Room generates the impl).
2. **Read-Flow accessors are named `get(...)` / `getById(...)`** — not `observe(...)`. They return `Flow<List<<X>Pack>>` for collections and `Flow<<X>Pack?>` for single records. The Repository layer renames them to `observe...` when re-exposed to the rest of the app.
3. **Mutations are `suspend`.** Two upsert patterns coexist, chosen by whether the entity has cascading children:
   - **`REPLACE` upsert** — for child entities and aggregate roots whose children are rewritten on every write. `@Insert(onConflict = OnConflictStrategy.REPLACE)` per entity (e.g. `insertNote`, `insertTags`, `insertItems`), composed by a `@Transaction suspend fun insertOrReplace(parent, children, grandchildren)` default-method that fans out the inserts in one DB transaction. Used for aggregate-root DAOs whose child rows are owned exclusively by the parent.
   - **Read-then-`@Insert` or `@Update`** — for parents whose children CASCADE on delete. `REPLACE` would drop and reinsert the parent row, taking the children with it. Instead: query for an existing row by id, then call `@Insert(onConflict = REPLACE) fun insert(...)` if absent or `@Update fun update(...)` if present, composed by a `@Transaction suspend fun insertOrUpdate(entity)` default-method. Used by `TokenDao`, `UserDao`, and any per-product DAO whose row owns CASCADE-linked children.
4. **`@Insert(onConflict = OnConflictStrategy.REPLACE)`** — upsert semantics. Server is the source of truth; if it sends a record with an existing ID, overwrite. When the entity has cascading children, pair it with `@Update` and the `insertOrUpdate` pattern to preserve the children across writes.
5. **`@Transaction` on `@Query` returning a `*Pack`** — Room executes the multi-table read in a single transaction. Without it, related rows could be inserted between the parent and child fetches.
6. **Raw SQL in `@Query`.** No DSL helpers. SQL is readable and lets you reason about query plans.
7. **Deletes use `@Query`** by predicate. `delete()` clears the whole table; `deleteById(id)` removes a single record; `deleteByCreatedAtRange` / `deleteByCreatedAtRangeExceptIds` handle range reconciliation. `@Delete` annotations are not used.
8. **`@Query` parameters use named placeholders** (`:from`, `:ids`) — positional placeholders are forbidden.

### Range observers

Range-bounded `get(from, to)` returns a `Flow<List<...>>` of the matching rows. When the user changes the range, the ViewModel pushes the new range to its `state`, and the next observation cycle uses the updated bounds.

For "all matching" queries (no range), expose `get()` with no args. Use sparingly — unbounded list observations are expensive.

### Range reconciliation

The `deleteByCreatedAtRangeExceptIds(from, to, ids)` pattern is critical for cached collections:

```kotlin
// inside NoteRepositoryImpl.getNotes()
val response = api.getNotes(start = startUtc, end = endUtc)
response.onSuccess { r ->
    val actualIds = r.mapNotNull { note -> provideNote(note) }
    if (actualIds.isEmpty()) {
        noteDao.deleteByCreatedAtRange(startUtc, endUtc)
    } else {
        noteDao.deleteByCreatedAtRangeExceptIds(startUtc, endUtc, actualIds)
    }
}
```

`provideNote(...)` performs the upsert (`insertOrReplace(note, tags, items)`) and returns the parent id. After insertion, anything in the range **not** returned by the server is deleted locally. Stops "deleted on another device" rows from lingering forever.

## `*Pack` models

Composite read models combining a parent entity with `@Relation`-loaded children. Live in `:data-services:database/models`.

```kotlin
public data class NotePack(
    @Embedded val note: NoteEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "noteId",
        entity = TagEntity::class,
    )
    val tags: List<TagPack> = emptyList(),
)

public data class TagPack(
    @Embedded val tag: TagEntity,

    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id",
    )
    val category: CategoryEntity? = null,

    @Relation(
        parentColumn = "id",
        entityColumn = "tagId",
        entity = ItemEntity::class,
    )
    val items: List<ItemEntity> = emptyList(),
)
```

### Rules

1. **`data class <X>Pack`** with `@Embedded` parent + one or more `@Relation` fields.
2. **`@Embedded` is the **parent** entity** — flattens fields into the SQL columns.
3. **`@Relation` for each child collection.** Room runs a separate query per relation, then groups by foreign key.
4. **Default `= emptyList()`** for relation lists. Default `= null` for optional one-to-one relations.
5. **Nested `*Pack`s** — `TagPack` references `CategoryEntity` via `@Relation` and includes `items`. Three levels of nesting is the practical limit; beyond that, query cost balloons.
6. **`@Transaction` is required on the DAO query** returning a `*Pack` (see DAO rules).
7. **Packs are read-only.** They're returned by DAO observers; mutations go through entity inserts/updates.

### Why `*Pack`

Without `@Relation`, you'd have to:

1. Observe `NoteEntity`.
2. For each note, observe `TagEntity` separately.
3. For each tag, observe `ItemEntity` separately.
4. Combine in Kotlin with `combine(...)`.

That's expensive (N+1 + reactive chaos) and fragile. `*Pack` + `@Relation` + `@Transaction` does it in one SQL transaction, returning a tree.

## TypeConverters

```kotlin
public class StringListConverter {
    @TypeConverter
    public fun fromList(list: List<String>): String = list.joinToString(separator = "|")

    @TypeConverter
    public fun toList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return value.split("|")
    }
}
```

Used for storing `List<String>` in a single column (e.g. tags, IDs). Registered on the `@Database` class via `@TypeConverters(StringListConverter::class)`.

**Pipe `|` is the delimiter**, not comma. Pipe is rare in user-generated strings; commas collide with natural language.

For more complex collection columns, add a JSON-based converter:

```kotlin
public class JsonStringListConverter {
    @TypeConverter
    public fun fromList(list: List<String>): String = Json.encodeToString(list)

    @TypeConverter
    public fun toList(value: String): List<String> = Json.decodeFromString(value)
}
```

But keep `StringListConverter` for simple pipe-delimited cases — it's smaller in storage.

## Anti-patterns

- **Nullable types on required domain attributes.** If a field must be populated for the entity to be valid, declare it non-null and let the DTO→Entity mapper reject invalid rows via `AppLogger.Mapping.log(value) ?: return null`. Reserve nullability for genuinely-optional fields and optional one-to-one parents (see rule 4).
- **`@PrimaryKey(autoGenerate = true) val id: Long`** — IDs come from the server.
- **Missing `@Index` on foreign-key columns** — slow joins.
- **Missing `@Transaction` on `@Query` returning a `*Pack`** — race conditions between parent + child reads.
- **Storing `LocalDateTime` as `Long` epoch** — `String` (ISO-8601 UTC) is the convention.
- **Nested `*Pack` deeper than three levels** — query explosion.
- **Mutating data through a `*Pack`** — packs are read-only views.
- **Storing JSON in a column** — Room is relational; if the structure is repeating, model it as a child table.
