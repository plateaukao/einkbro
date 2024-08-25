package info.plateaukao.einkbro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.database.ChatGptQuery
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GptQueryViewModel : ViewModel(), KoinComponent {
    private val bookmarkManager: BookmarkManager by inject()

    fun getGptQueries() = bookmarkManager.getAllChatGptQueries()

//    suspend fun getGptQuery(gptQueryId: Int) = bookmarkManager.getChatGptQueryById(gptQueryId)

    suspend fun dumpGptQueriesAsHtml(): String {
        val sb = StringBuilder()

        // Add the HTML structure with UTF-8 encoding
        sb.append(
            """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>GPT Queries Dump</title>
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; margin: 20px; }
                .query { margin-bottom: 20px; }
                .query-text { font-weight: bold; }
            </style>
            <script type="module" src="https://md-block.verou.me/md-block.js"></script>
        </head>
        <body>
        <h1>GPT Queries</h1>
    """.trimIndent()
        )

        // Collect the queries and format them
        bookmarkManager.getAllChatGptQueriesAsync().forEach {
            sb.append(
                """
            <div class="query">
            <hr>
                <h2>${it.selectedText.replace("<<","(").replace(">>", ")")}</h2>
                <md-block hmin="3">${it.result}</md-block>
            </div>
        """.trimIndent()
            )
        }

        // Close the HTML structure
        sb.append(
            """
        </body>
        </html>
    """.trimIndent()
        )

        return sb.toString()
    }

    fun deleteGptQuery(query: ChatGptQuery) {
        viewModelScope.launch {
            bookmarkManager.deleteChatGptQuery(query)
        }
    }
}
