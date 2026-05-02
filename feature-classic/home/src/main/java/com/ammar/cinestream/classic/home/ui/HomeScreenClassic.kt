package com.ammar.cinestream.classic.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.ammar.cinestream.classic.home.viewmodel.HomeClassicViewModel


@Composable
fun HomeScreenClassic(modifier: Modifier = Modifier,
                      viewModel: HomeClassicViewModel = hiltViewModel(),
                      onNavigateToDetail: (String) -> Unit,
                      onNavigateToList: (String) -> Unit,
                      onNavigateToSearch: () -> Unit
) {

}