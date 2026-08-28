package com.netonstream.privchat.ui.components

import androidx.compose.runtime.Composable
import com.gearui.components.loading.Loading
import com.gearui.components.loading.LoadingSize
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier

/**
 * 页面级加载态：内容区正中一个 loading 指示器。
 *
 * 所有「进页面先拉数据」的页面统一用它，不再各写各的「加载中…」文字或干脆留一块
 * 空白（空白页在暗色下就是一屏黑，用户分不清是在加载还是坏了）。
 *
 * 放在 NavBar 之后、占满剩余空间：
 * ```
 * Column {
 *     NavBar(...)
 *     when {
 *         loading -> PageLoading()
 *         ...
 *     }
 * }
 * ```
 */
@Composable
fun PageLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Loading(size = LoadingSize.LARGE)
    }
}
