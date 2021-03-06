/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

package net.mamoe.mirai.api.http

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.api.http.MiraiHttpAPIServer.logger
import net.mamoe.mirai.api.http.config.Setting
import net.mamoe.mirai.api.http.service.MiraiApiHttpServices
import net.mamoe.mirai.utils.BotConfiguration
import net.mamoe.yamlkt.Yaml
import java.io.File

internal typealias CommandSubscriber = suspend (String, Long, Long, List<String>) -> Unit

object HttpApi{
    var services: MiraiApiHttpServices = MiraiApiHttpServices()
    val dataFolder = File(".")
    var bot: Bot? = null
    @JvmStatic
    fun main(args: Array<String>){
        Runtime.getRuntime().addShutdownHook(Thread{
            if (bot!=null){
                logger.info("Shutting down....")
                bot!!.close()
            }
        });
        runBlocking {
            logger.info("Mirai API Http Standalone Edition LOADING")
            val qq = System.getProperty("qq")
            val pwd = System.getProperty("pwd")
            lateinit var user: UserConfiguration
            if(qq != null && pwd !=null){
                user = UserConfiguration(qq.toLong(),pwd)
            }else {
                val configuration = File("user.yml")
                if (!configuration.exists()) {
                    // init and quit
                    user = UserConfiguration(2333333, "your password here");
                    configuration.writeText(Yaml.encodeToString(user))
                    logger.warning("请前往 user.yml 进行用户信息配置...")
                    System.exit(0);
                }
                user = Yaml.decodeFromString(UserConfiguration.serializer(), configuration.readText())
            }
            kotlin.runCatching {
                BotFactory.newBot(user.qq,user.pwd){
                    fileBasedDeviceInfo("device.json")
                    protocol = user.protocol
                }.alsoLogin()
            }.onSuccess {
                bot=it
            }.onFailure {
                logger.error(it)
                logger.error("FAILED TO LOGIN")
            }
            onEnable()
        }
    }
    fun onEnable() {
        Setting.global.reload()

        with(Setting.global) {

            if (authKey.startsWith("INITKEY")) {
                logger.warning("USING INITIAL KEY, please edit the key")
            }

            logger.info("Starting Mirai HTTP Server in $host:$port")
            services.onLoad()

            MiraiHttpAPIServer.start(host, port, authKey)

            services.onEnable()
        }
    }

    fun onDisable() {
        MiraiHttpAPIServer.stop()

        services.onDisable()
    }

    private val subscribers = mutableListOf<CommandSubscriber>()

    internal fun subscribeCommand(subscriber: CommandSubscriber): CommandSubscriber =
        subscriber.also { subscribers.add(it) }

    internal fun unSubscribeCommand(subscriber: CommandSubscriber) = subscribers.remove(subscriber)

    private val imageFold: File = File(dataFolder, "images").apply { mkdirs() }

    internal fun image(imageName: String) = File(imageFold, imageName)

    fun saveImageAsync(name: String, data: ByteArray) =
        GlobalScope.async { //不会改...
            image(name).apply { writeBytes(data) }
        }

    private val voiceFold: File = File(dataFolder, "voices").apply { mkdirs() }

    internal fun voice(voiceName: String) = File(voiceFold, voiceName)

    fun saveVoiceAsync(name: String, data: ByteArray) =
        GlobalScope.async {
            voice(name).apply { writeBytes(data) }
        }

    private val fileFold: File = File(dataFolder, "file").apply { mkdirs() }

    internal fun file(fileName: String) = File(fileFold, fileName)

    fun saveFileAsync(name: String, data: ByteArray) =
        GlobalScope.async {
            file(name).apply { writeBytes(data) }
        }

}
