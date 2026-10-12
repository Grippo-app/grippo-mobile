package com.grippo.authorization.analytics

import com.grippo.analytics.common.models.InputStatus
import com.grippo.analytics.authorization.models.ProfileExperience
import com.grippo.core.state.formatters.EmailFormatState
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.NameFormatState
import com.grippo.core.state.formatters.PasswordFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.profile.ExperienceEnumState
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthInputMappingTest {
    @Test
    fun credentialsAndProfileInputsExposeOnlyValidationStatus() {
        assertEquals(InputStatus.Empty, AuthInputMapping.status(EmailFormatState.Empty()))
        assertEquals(InputStatus.Invalid, AuthInputMapping.status(EmailFormatState.of("private")))
        assertEquals(InputStatus.Valid, AuthInputMapping.status(EmailFormatState.of("person@example.com")))
        assertEquals(InputStatus.Empty, AuthInputMapping.status(PasswordFormatState.Empty()))
        assertEquals(InputStatus.Invalid, AuthInputMapping.status(PasswordFormatState.of("short")))
        assertEquals(InputStatus.Valid, AuthInputMapping.status(PasswordFormatState.of("private-password")))
        assertEquals(InputStatus.Empty, AuthInputMapping.status(NameFormatState.Empty()))
        assertEquals(InputStatus.Invalid, AuthInputMapping.status(NameFormatState.of("n".repeat(61))))
        assertEquals(InputStatus.Valid, AuthInputMapping.status(NameFormatState.of("Person")))
        assertEquals(InputStatus.Empty, AuthInputMapping.status(HeightFormatState.Empty()))
        assertEquals(InputStatus.Invalid, AuthInputMapping.status(HeightFormatState.of(99)))
        assertEquals(InputStatus.Valid, AuthInputMapping.status(HeightFormatState.of(175)))
        assertEquals(InputStatus.Empty, AuthInputMapping.status(WeightFormatState.Empty()))
        assertEquals(InputStatus.Invalid, AuthInputMapping.status(WeightFormatState.of(10f)))
        assertEquals(InputStatus.Valid, AuthInputMapping.status(WeightFormatState.of(70f)))
    }

    @Test
    fun everyExperienceHasOneBoundedValue() {
        assertEquals(
            ProfileExperience.entries,
            ExperienceEnumState.entries.map(AuthInputMapping::experience),
        )
    }
}
