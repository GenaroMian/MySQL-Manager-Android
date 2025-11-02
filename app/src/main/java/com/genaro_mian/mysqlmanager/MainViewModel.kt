package com.genaro_mian.mysqlmanager // (Seu pacote)

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// 1. O ViewModel
// Ele recebe o "operário" (DAO) e faz o trabalho lógico
class MainViewModel(private val conexaoDao: ConexaoDao) : ViewModel() {

    // Expõe a lista "viva" de conexões para a UI
    val conexoes: Flow<List<ConexaoSalva>> = conexaoDao.getTodasConexoes()

    // Função que a UI vai chamar para deletar uma conexão
    // Ela usa o 'viewModelScope' para rodar numa Coroutine
    fun deletarConexao(conexao: ConexaoSalva) {
        viewModelScope.launch(Dispatchers.IO) {
            conexaoDao.deletarPeloId(conexao.id)
        }
    }
}

// 2. A "Fábrica" (Factory)
// Como o nosso ViewModel precisa do DAO para ser criado,
// precisamos de uma "Fábrica" para construí-lo.
class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            // Pega o DAO e "injeta" ele no ViewModel
            val dao = AppDatabase.getDatabase(application).conexaoDao()
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}