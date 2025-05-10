package com.example.imapp.nav

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.imapp.HomeScreen
import com.example.imapp.ui.screen.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootNavGraph() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "tabs") {

        /* 一级：底部 Tab 容器 */
        composable("tabs") { HomeScreen(navController = nav) }

        /* 二级：音色管理 */
        composable("voice-management") { VoiceManagementScreen(nav) }

        /* 三级：创建音色 */
        composable("voice-create") { VoiceCreateScreen(nav) }

        // ✅ 新增登录页面
        composable("login") {
            LoginScreen(onLoginSuccess = {
                nav.popBackStack()  // 登录成功返回上一页
            })
        }
    }
}
