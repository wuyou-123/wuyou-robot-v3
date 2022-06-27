# wuyou-robot-v3

一个基于[Simply Robot](https://github.com/ForteScarlet/simpler-robot/tree/v3-dev)
开发的bot项目, [v2](https://github.com/wuyou-123/wuyou-robot/)

# 这里是游戏的文档

## 如何新建一个游戏?

1. 实现一些接口(其实是抽象方法):

   [Game](/src/main/kotlin/pers/wuyou/robot/game/common/interfaces/Game.kt)

   [Room](/src/main/kotlin/pers/wuyou/robot/game/common/interfaces/Room.kt)

   [Player](/src/main/kotlin/pers/wuyou/robot/game/common/interfaces/Player.kt)

比如这样:

```kotlin
/**
 * 游戏对象, 属性信息可以参考Game类
 */
@Component
class MyGame : Game<MyGame, MyGameRoom, MyGamePlayer>() {
    override val name = "我的游戏"
    override val minPlayerCount = 2
    override val maxPlayerCount = 2
    override val canMultiRoom = true
}

/**
 * 游戏房间, 这里维护了玩家列表, 可以在这里实现一些房间内常用的事件(有些事件不需要重写)
 */
class MyGameRoom(
    override var id: String,
    override var name: String,
    override val game: MyGame = GameManager.getGame(),
) : Room<MyGame, MyGamePlayer, MyGameRoom>() {
    override fun playerFull() {
        // 房间内玩家已满的事件, 可以用于修改房间状态等操作
    }
    override fun createRoom() {
        // 新建房间事件
    }
    override fun join(player: GobangPlayer) {
        // 玩家 $player 加入了房间
    }
    override fun destroy() {
        // 玩家为空, 房间会自动销毁
    }
    override fun leave(player: GobangPlayer) {
        // 玩家 $player 离开了房间
    }
    override fun otherMessage(messageContent: MessageContent) {
        // 收到其他消息的事件, 比如说可以写一个私聊聊天?
    }
}

/**
 * 玩家对象
 */
class MyGamePlayer(
    override var id: String,
    override var name: String,
    override var room: MyGameRoom
) : Player<MyGame, MyGameRoom, MyGamePlayer>() {
    override fun getStatus(): GameStatus {
        // 这里需要返回一个GameStatus对象,用来识别游戏事件
        return GameStatus("")
    }
}

```

2. 新建一些游戏事件类(游戏事件需要实现[GameEvent](/src/main/kotlin/pers/wuyou/robot/game/common/interfaces/GameEvent.kt))

比如这样:

```kotlin
class MyEvent : GameEvent<MyGame, MyGameRoom, MyGamePlayer>() {
    override val matcher: GameEventMatcher = GameEventMatcher { msg, gameArg ->
        // 这里是该事件的匹配方法, 参数是收到的消息(MessageContent)
        // 当该方法返回true时, 会执行下面的invoke方法
        return true
    }

    override suspend fun invoke(e: Any) {
        // 游戏事件
    }

    override fun getStatus(): GameStatus {
        // 游戏事件绑定的状态, GameStatus的status属性和Player的getStatus()方法的返回值绑定
        return GameStatus("")
    }
}
```

## 游戏框架实现逻辑?

1. 项目启动后会载入所有实现了`Game`和`GameEvent`的类
2. 根据游戏事件的泛型将所有游戏事件载入到游戏对象的`eventMap`中
3. 收到消息后,会根据`player`的`getStatus`方法获取当前玩家的`GameStatus`对象,通过这个对象获取当前游戏的事件并尝试执行

## 一些好用的建议

1. `GameEvent`提供了一些方法,比如说你可以通过`sendRoomAndWaitPlayerNext`来实现定时等待该玩家的下一条消息的逻辑,当执行这个方法后,会挂起并等待该玩家的下一条消息,注意:这条消息并不会执行事件监听器
2. 项目中提供了`Timer`类,可以用来实现定时任务并再任务中添加事件

```kotlin
// 倒计时120秒的示例
val gameArg = GameArg()
Timer(120, TimeUnit.SECONDS, gameArg, true) {
    onStart { println("倒计时开始!") }
    60 { println("还剩下一分钟!") }
    90 { println("只剩下三十秒了!") }
    onFinish { println("倒计时结束了!") }
}
```