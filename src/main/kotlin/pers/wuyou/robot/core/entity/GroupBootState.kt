package pers.wuyou.robot.core.entity

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.int
import org.ktorm.schema.varchar

/**
 * 群开关机状态
 *
 * @author wuyou
 * @since 2021-08-05
 */
interface GroupBootState : Entity<GroupBootState>{
    var id: Int
    /**
     * 开关机状态
     */
    var state: Boolean
    /**
     * 群号
     */
    var groupCode: String
}

object GroupBootStates : Table<GroupBootState>("group_boot_state") {
    val id = int("id").primaryKey().bindTo { it.id }
    val groupCode = varchar("group_code").bindTo { it.groupCode }
    val state = boolean("state").bindTo { it.state }
}
val Database.groupBootStates get() = this.sequenceOf(GroupBootStates)