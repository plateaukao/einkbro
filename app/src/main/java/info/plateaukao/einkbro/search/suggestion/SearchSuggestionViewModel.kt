package info.plateaukao.einkbro.search.suggestion

import info.plateaukao.einkbro.database.Record
import info.plateaukao.einkbro.database.RecordDb
import info.plateaukao.einkbro.database.RecordType
import info.plateaukao.einkbro.preference.ConfigManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SearchSuggestionViewModel : KoinComponent {
    private val config: ConfigManager by inject()
    private val recordDb: RecordDb by inject()

    private val repository: SearchSuggestionsRepository by lazy {
        when (config.searchEngine) {
            "Google" -> GoogleSuggestionsRepository()
            else -> GoogleSuggestionsRepository()
        }
    }

    // create mutable state flow variable for suggestions
    private val _suggestions = MutableStateFlow<List<Record>>(emptyList())
    val suggestions: StateFlow<List<Record>> = _suggestions

    private var historyAndBookmarkRecords = listOf<Record>()
    private var queryString = ""
    suspend fun initSuggestions() {
        historyAndBookmarkRecords = recordDb.listEntries(config.showBookmarksInInputBar)
        _suggestions.value = historyAndBookmarkRecords
        queryString = ""
    }

    suspend fun updateSuggestions(query: String) {
        if (query.isEmpty()) {
            _suggestions.value = historyAndBookmarkRecords
            return
        }

        if (queryString.isNotEmpty() && query.startsWith(queryString) && _suggestions.value.isEmpty())
        {
            // if the new query is an extension of the previous query and previous suggestions are empty, keep it empty
            queryString = query
            return
        }

        queryString = query

        val filteredRecords = historyAndBookmarkRecords.filter {
            it.title?.contains(query, ignoreCase = true) == true ||
                    it.url.contains(query, ignoreCase = true)
        }

        if ((query.length <= 1 && filteredRecords.isNotEmpty()) || !config.enableSearchSuggestion) {
            _suggestions.value = filteredRecords
            return
        }

        try {
            val results = repository.searchSuggestionResults(query).take(6)
            _suggestions.value =
                results.map { Record(title = it.title, url = it.url, time = -1, type = RecordType.Suggestion) } +
                        filteredRecords
        } catch (e: Exception) {
            e.printStackTrace()
            _suggestions.value = emptyList()
        }
    }

}