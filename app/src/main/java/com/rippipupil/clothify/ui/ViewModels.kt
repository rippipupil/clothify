package com.rippipupil.clothify.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/** Crea un ViewModel con dependencias del AppContainer. */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(crossinline create: () -> VM): VM =
    viewModel(factory = viewModelFactory { initializer { create() } })
