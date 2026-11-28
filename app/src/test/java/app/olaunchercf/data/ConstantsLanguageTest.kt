package app.olaunchercf.data

import app.olaunchercf.data.Constants.value
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstantsLanguageTest {

    @Test
    fun nepaliLanguageIsAvailableWithNeLocaleCode() {
        assertTrue(Constants.Language.values().contains(Constants.Language.Nepali))
        assertEquals("ne", Constants.Language.Nepali.value())
    }
}
