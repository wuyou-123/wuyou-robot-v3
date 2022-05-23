package pers.wuyou.robot.entertainment.entity

import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

/**
 * 戳一戳发送的内容
 * sentence
 *
 * @author wuyou
 */
interface Sentence : Entity<Sentence> {
    val id: Int
    var text: String
}
object Sentences : Table<Sentence>("sentence") {
    val id = int("id").primaryKey().bindTo { it.id }
    val text = varchar("text").bindTo { it.text }

}

val Database.sentences get() = this.sequenceOf(Sentences)