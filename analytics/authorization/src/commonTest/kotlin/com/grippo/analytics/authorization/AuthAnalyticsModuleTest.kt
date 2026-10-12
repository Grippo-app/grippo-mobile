package com.grippo.analytics.authorization

import com.grippo.analytics.authorization.internal.schema.AuthInteractionSchema
import com.grippo.analytics.authorization.internal.schema.AuthOperationSchema
import com.grippo.toolkit.analytics.schema.AnalyticsSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import org.koin.dsl.koinApplication
import org.koin.ksp.generated.module

class AuthAnalyticsModuleTest {
    @Test
    fun annotatedModuleContributesExactlyTheFeatureSchemaMatrix() {
        val application = koinApplication { modules(AuthAnalyticsModule().module) }
        try {
            val schemas = application.koin.getAll<AnalyticsSchema>()
            assertEquals(1, schemas.size)
            assertEquals(AuthOperationSchema.all + AuthInteractionSchema.all, schemas.single().events)
            assertEquals(22, schemas.single().events.size)
            assertEquals(22, schemas.single().events.map { it.name }.distinct().size)
        } finally {
            application.close()
        }
    }
}
