package com.example.imapp.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.imapp.repository.AuthRepository

@Composable
fun MeScreen(modifier: Modifier = Modifier,  onLoginClick: () -> Unit = {}) {

    val context = LocalContext.current
    var isLoggedIn by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("未登录") }

    // 加载用户状态
    LaunchedEffect(Unit) {
        val token = AuthRepository.getToken(context)
        isLoggedIn = !token.isNullOrBlank()
        username = if (isLoggedIn) "已登录" else "未登录"
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("个人中心", modifier = Modifier.padding(vertical = 8.dp))
        HorizontalDivider()

        Text(
            text = if (isLoggedIn) "$username（点击退出）" else "点击登录",
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (isLoggedIn) {
                        AuthRepository.saveToken(context, "") // 清空 Token
                        isLoggedIn = false
                        username = "未登录"
                    } else {
                        onLoginClick()
                    }
                }
                .padding(vertical = 8.dp)
        )

        HorizontalDivider()

        Text("设置", modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // TODO: 跳转"设置"页 or 弹窗
            }
            .padding(vertical = 8.dp)
        )
        HorizontalDivider()
        Text("帮助与反馈", modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // TODO: 打开帮助
            }
            .padding(vertical = 8.dp)
        )
        HorizontalDivider()

        Text("关于", modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // TODO: 弹窗 or 新页面
            }
            .padding(vertical = 8.dp)
        )
        Divider()
    }
}


/** 预览函数：显示 MeScreen 布局 */
@Preview(showBackground = true, name = "MeScreenPreview")
@Composable
fun MeScreenPreview() {
    MeScreen()
}