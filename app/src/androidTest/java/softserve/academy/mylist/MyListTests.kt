package softserve.academy.mylist

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MyListTestsListTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = ShoppingDatabase.getInstance(context)
        runBlocking(Dispatchers.IO) {
            db.shoppingDao().deleteAll()
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun addItem_test() {
        composeTestRule.onNodeWithTag("counter_text").assertExists()
        composeTestRule.onNodeWithTag("input_field").performTextInput("Молоко")
        composeTestRule.onNodeWithTag("add_button").performClick()

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Молоко").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Молоко").assertIsDisplayed()
        composeTestRule.onNodeWithTag("counter_text").assertTextContains("0 / 1", substring = true)
        composeTestRule.onNodeWithTag("input_field").assertTextContains("", substring = true)
    }

    @Test
    fun deleteItem_test() {
        composeTestRule.onNodeWithTag("input_field").performTextInput("Хліб")
        composeTestRule.onNodeWithTag("add_button").performClick()

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithContentDescription("Delete").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onAllNodesWithContentDescription("Delete").onFirst().performClick()

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Хліб").fetchSemanticsNodes().isEmpty()
        }

        composeTestRule.onNodeWithText("Хліб").assertDoesNotExist()
        composeTestRule.onNodeWithTag("counter_text").assertTextContains("0 / 0", substring = true)
    }

    @Test
    fun persistence_test() {
        composeTestRule.onNodeWithTag("input_field").performTextInput("Яблуко")
        composeTestRule.onNodeWithTag("add_button").performClick()

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Яблуко").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.onNodeWithText("Яблуко").assertIsDisplayed()
    }
}