package info.plateaukao.einkbro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.plateaukao.einkbro.service.InstapaperRepository
import info.plateaukao.einkbro.service.InstapaperResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class InstapaperViewModel : ViewModel(), KoinComponent {
    
    private val instapaperRepository: InstapaperRepository by inject()
    
    private val _uiState = MutableStateFlow(InstapaperUiState())
    val uiState: StateFlow<InstapaperUiState> = _uiState.asStateFlow()
    
    fun addUrl(
        url: String,
        username: String,
        password: String,
        title: String? = null
    ) {
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "URL is empty"
            )
            return
        } else if (username.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                showConfigureDialog = true,
                errorMessage = "Username and password are required"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            when (val result = instapaperRepository.addUrl(url, username, password, title)) {
                is InstapaperResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = result.message,
                        errorMessage = null
                    )
                }
                is InstapaperResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message,
                        successMessage = null
                    )
                }
            }
        }
    }
    
    fun authenticate(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                showConfigureDialog = true,
                errorMessage = "Username and password are required"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            when (val result = instapaperRepository.authenticate(username, password)) {
                is InstapaperResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showConfigureDialog = false,
                        successMessage = result.message,
                        errorMessage = null
                    )
                }
                is InstapaperResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showConfigureDialog = false,
                        errorMessage = result.message,
                        successMessage = null
                    )
                }
            }
        }
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }
    
    fun resetState() {
        _uiState.value = InstapaperUiState()
    }
}

data class InstapaperUiState(
    val isLoading: Boolean = false,
    val showConfigureDialog: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)