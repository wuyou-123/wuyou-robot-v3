package pers.wuyou.robot

import love.forte.simboot.autoconfigure.EnableSimbot
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@EnableSimbot
@SpringBootApplication
class WuyouRobotV3Application

fun main(args: Array<String>) {
    runApplication<WuyouRobotV3Application>(*args)
}
