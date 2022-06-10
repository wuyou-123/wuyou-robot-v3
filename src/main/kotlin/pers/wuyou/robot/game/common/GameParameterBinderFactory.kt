package pers.wuyou.robot.game.common

import love.forte.di.annotation.Beans
import love.forte.simboot.listener.ParameterBinder
import love.forte.simboot.listener.ParameterBinderFactory
import love.forte.simboot.listener.ParameterBinderResult
import love.forte.simbot.attribute
import love.forte.simbot.event.EventListenerProcessingContext
import kotlin.reflect.full.findAnnotation

@Beans
class GameParameterBinderFactory : ParameterBinderFactory {
    override fun resolveToBinder(context: ParameterBinderFactory.Context): ParameterBinderResult {

        // 寻找参数上的注解，找不到则跳过
        val attrAnnotation = context.parameter.findAnnotation<GameAttr>() ?: return ParameterBinderResult.empty()

        // 构建binder
        // 细节问题自己处理，可空处理等。
        return ParameterBinderResult.normal(AttrBinder(attrAnnotation.value))

    }

    private class AttrBinder(attrName: String) : ParameterBinder {
        private val attr = attribute<Any>(attrName)
        override suspend fun arg(context: EventListenerProcessingContext): Result<Any?> {
            // 从上下文尝试获取目标
            // 此处无视任何细节，直接尝试返回
            return kotlin.runCatching { context[attr] }
        }
    }
}

annotation class GameAttr(val value: String)
