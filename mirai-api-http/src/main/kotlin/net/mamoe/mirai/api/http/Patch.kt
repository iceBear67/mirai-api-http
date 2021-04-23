package net.mamoe.mirai.api.http

import kotlinx.serialization.Serializable
import net.mamoe.mirai.utils.BotConfiguration

@Serializable
data class UserConfiguration(
    val qq: Long,
    val pwd: String,
    val protocol: BotConfiguration.MiraiProtocol = BotConfiguration.MiraiProtocol.ANDROID_PAD
)