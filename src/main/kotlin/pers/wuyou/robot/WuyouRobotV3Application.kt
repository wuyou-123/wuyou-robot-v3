package pers.wuyou.robot

import love.forte.simboot.spring.autoconfigure.EnableSimbot
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import pers.wuyou.robot.music.config.MusicProperties

@EnableSimbot
@SpringBootApplication
@EnableConfigurationProperties(MusicProperties::class)
class WuyouRobotV3Application

fun main(args: Array<String>) {
    runApplication<WuyouRobotV3Application>(*args)
}
