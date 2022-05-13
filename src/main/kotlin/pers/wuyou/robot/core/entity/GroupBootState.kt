package pers.wuyou.robot.core.entity

import org.ktorm.dsl.QueryRowSet
import org.ktorm.schema.BaseTable
import org.ktorm.schema.boolean
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import java.io.Serializable

/**
 * 群开关机状态
 *
 * @author wuyou
 * @since 2021-08-05
 */
class GroupBootState(
    var id: Int?,
    /**
     * 开关机状态
     */
    var state: Boolean,
    /**
     * 群号
     */
    var groupCode: String
) : Serializable {}

object GroupBootStates : BaseTable<GroupBootState>("group_boot_state") {
    val id = int("id").primaryKey()
    val groupCode = varchar("group_code")
    val state = boolean("state")

    override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = GroupBootState(
        id = row[id] ?: 0, groupCode = row[groupCode] ?: "", state = row[state] ?: false
    )
}