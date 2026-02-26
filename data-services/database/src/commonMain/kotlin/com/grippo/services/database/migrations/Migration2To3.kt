package com.grippo.services.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

internal object Migration2To3 : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exercise_example_search_prefix` (
                `exerciseExampleId` TEXT NOT NULL,
                `token` TEXT NOT NULL,
                `prefix` TEXT NOT NULL,
                PRIMARY KEY(`exerciseExampleId`, `token`, `prefix`),
                FOREIGN KEY(`exerciseExampleId`) REFERENCES `exercise_example`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        connection.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_exercise_example_search_prefix_exerciseExampleId`
            ON `exercise_example_search_prefix` (`exerciseExampleId`)
            """.trimIndent()
        )

        connection.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_exercise_example_search_prefix_prefix`
            ON `exercise_example_search_prefix` (`prefix`)
            """.trimIndent()
        )

        // Backfill prefix index for existing records using SQL-only tokenization.
        // Mirrors the runtime search index strategy with minimum prefix length = 3.
        connection.execSQL(
            """
            WITH RECURSIVE
            normalized(exerciseExampleId, value) AS (
                SELECT
                    `id`,
                    TRIM(
                        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                            LOWER(`name`),
                            '-', ' '),
                            '_', ' '),
                            '/', ' '),
                            '\', ' '),
                            '.', ' '),
                            ',', ' '),
                            '(', ' '),
                            ')', ' '),
                            '[', ' '),
                            ']', ' '),
                            '+', ' '),
                            ':', ' ')
                    )
                FROM `exercise_example`
            ),
            tokens(exerciseExampleId, rest, token) AS (
                SELECT
                    exerciseExampleId,
                    CASE
                        WHEN INSTR(value, ' ') = 0 THEN ''
                        ELSE SUBSTR(value, INSTR(value, ' ') + 1)
                    END,
                    CASE
                        WHEN INSTR(value, ' ') = 0 THEN value
                        ELSE SUBSTR(value, 1, INSTR(value, ' ') - 1)
                    END
                FROM normalized
                WHERE value <> ''

                UNION ALL

                SELECT
                    exerciseExampleId,
                    CASE
                        WHEN INSTR(rest, ' ') = 0 THEN ''
                        ELSE SUBSTR(rest, INSTR(rest, ' ') + 1)
                    END,
                    CASE
                        WHEN INSTR(rest, ' ') = 0 THEN rest
                        ELSE SUBSTR(rest, 1, INSTR(rest, ' ') - 1)
                    END
                FROM tokens
                WHERE rest <> ''
            ),
            filtered_tokens AS (
                SELECT DISTINCT exerciseExampleId, token
                FROM tokens
                WHERE token <> '' AND LENGTH(token) >= 3
            ),
            prefixes(exerciseExampleId, token, len) AS (
                SELECT exerciseExampleId, token, 3
                FROM filtered_tokens

                UNION ALL

                SELECT exerciseExampleId, token, len + 1
                FROM prefixes
                WHERE len < LENGTH(token)
            )
            INSERT OR IGNORE INTO `exercise_example_search_prefix` (`exerciseExampleId`, `token`, `prefix`)
            SELECT
                exerciseExampleId,
                token,
                SUBSTR(token, 1, len)
            FROM prefixes
            """.trimIndent()
        )
    }
}
