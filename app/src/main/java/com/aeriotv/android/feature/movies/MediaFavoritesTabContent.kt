package com.aeriotv.android.feature.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.aeriotv.android.core.preferences.WatchlistStore
import com.aeriotv.android.ui.tv.tvFocusScale

/**
 * Favorites tab: every movie / series the user starred (the same list the
 * Watchlist shelf shows, stored in [WatchlistStore]). A Movies | TV Shows
 * switch at the top picks which kind to browse; OK on a poster opens that
 * title's detail page (where Play lives) and OK on the star under it removes
 * the title from Favorites. Everything is D-pad focusable.
 */
@Composable
fun MediaFavoritesTabContent(
    onMovieClick: (String) -> Unit,
    onSeriesClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    watchlistVm: WatchlistViewModel = hiltViewModel(),
) {
    val entries by watchlistVm.entries.collectAsStateWithLifecycle(initialValue = emptyList())
    var showMovies by rememberSaveable { mutableStateOf(true) }
    val shown = remember(entries, showMovies) { entries.filter { it.isMovie == showMovies } }

    Column(modifier = modifier.fillMaxSize().padding(top = 12.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FavoritesKindPill("Movies", selected = showMovies, onClick = { showMovies = true })
            FavoritesKindPill("TV Shows", selected = !showMovies, onClick = { showMovies = false })
        }
        Spacer(Modifier.height(12.dp))
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (showMovies) {
                        "No favorite movies yet.\nOpen a movie and choose Favorite."
                    } else {
                        "No favorite TV shows yet.\nOpen a show and choose Favorite."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 130.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(shown, key = { it.key }) { entry ->
                    FavoriteCard(
                        entry = entry,
                        onOpen = {
                            if (entry.isMovie) {
                                onMovieClick(entry.key.removePrefix("m:"))
                            } else {
                                entry.key.removePrefix("s:").toIntOrNull()?.let(onSeriesClick)
                            }
                        },
                        onUnfavorite = { watchlistVm.remove(entry.key) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritesKindPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .tvFocusScale(focused, focusedScale = 1.04f)
            .clip(CircleShape)
            .background(if (selected) colors.primary else colors.onSurface.copy(alpha = 0.12f))
            // White ring on the filled (selected) pill, accent ring otherwise,
            // like the detail-page action buttons.
            .border(
                1.5.dp,
                when {
                    !focused -> Color.Transparent
                    selected -> Color.White
                    else -> colors.primary
                },
                CircleShape,
            )
            .combinedClickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) colors.onPrimary else colors.onSurface,
        )
    }
}

@Composable
private fun FavoriteCard(
    entry: WatchlistStore.Entry,
    onOpen: () -> Unit,
    onUnfavorite: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val posterSource = remember { MutableInteractionSource() }
    val posterFocused by posterSource.collectIsFocusedAsState()
    val starSource = remember { MutableInteractionSource() }
    val starFocused by starSource.collectIsFocusedAsState()
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .tvFocusScale(posterFocused, focusedScale = 1.05f)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
                .border(2.dp, if (posterFocused) colors.primary else Color.Transparent, RoundedCornerShape(8.dp))
                .combinedClickable(interactionSource = posterSource, indication = null, onClick = onOpen),
        ) {
            if (!entry.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = entry.posterUrl,
                    contentDescription = entry.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = entry.title,
                    color = colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Center).padding(8.dp),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Filled star = in Favorites. OK removes it.
            Box(
                modifier = Modifier
                    .tvFocusScale(starFocused, focusedScale = 1.15f)
                    .clip(CircleShape)
                    .border(1.5.dp, if (starFocused) colors.primary else Color.Transparent, CircleShape)
                    .combinedClickable(interactionSource = starSource, indication = null, onClick = onUnfavorite)
                    .padding(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Remove ${entry.title} from Favorites",
                    tint = colors.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = entry.title,
                fontSize = 11.sp,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
