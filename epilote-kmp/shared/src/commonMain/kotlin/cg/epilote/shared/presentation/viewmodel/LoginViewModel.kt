package cg.epilote.shared.presentation.viewmodel

import cg.epilote.shared.domain.model.UserSession
import cg.epilote.shared.domain.usecase.auth.LoginResult
import cg.epilote.shared.domain.usecase.auth.LoginUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val session: UserSession) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
    object NoNetwork : LoginUiState()
}

class LoginViewModel(private val loginUseCase: LoginUseCase) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    fun onUsernameChange(value: String) { _username.value = value }
    fun onPasswordChange(value: String) { _password.value = value }

    fun login(pin: String) {
        val u = _username.value.trim()
        val p = _password.value

        if (u.isBlank() || p.isBlank()) {
            _uiState.value = LoginUiState.Error("Veuillez saisir votre identifiant et mot de passe")
            return
        }
        if (pin.length < 4) {
            _uiState.value = LoginUiState.Error("Le PIN doit contenir au moins 4 chiffres")
            return
        }

        _uiState.value = LoginUiState.Loading
        scope.launch {
            _uiState.value = when (val result = loginUseCase.execute(u, p, pin)) {
                is LoginResult.Success  -> LoginUiState.Success(result.session)
                is LoginResult.Error    -> LoginUiState.Error(result.message)
                is LoginResult.NoNetwork -> LoginUiState.NoNetwork
            }
        }
    }

    fun resetState() { _uiState.value = LoginUiState.Idle }
}
