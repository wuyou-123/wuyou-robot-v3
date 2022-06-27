package pers.wuyou.robot.game.idiom

import pers.wuyou.robot.core.common.isNull


@Suppress("unused")
class IdiomFactory() {
    /**
     * 成语列表
     */
    private val idiomList = mutableListOf<Idiom>()
    val size get() = idiomList.size

    /**
     * 成语字典
     */
    private val idiomWordMap = mutableMapOf<String, Idiom>()

    /**
     * 成语首字符map
     */
    private val firstIdiomMap = mutableMapOf<String, MutableList<Idiom>>()

    /**
     * 成语末字符map
     */
    private val endIdiomMap = mutableMapOf<String, MutableList<Idiom>>()

    /**
     * 未使用的成语列表
     */
    private lateinit var unusedIdiomList: MutableList<Idiom>

    /**
     * 添加成语
     */
    fun add(idiom: Idiom) {
        // 如果已经存在，则不添加
        idiomWordMap[idiom.word]?.let { return }
        // 添加到列表中
        idiomList.add(idiom)
        // 添加到字典中
        idiomWordMap[idiom.word] = idiom
        // 添加到首字符map
        firstIdiomMap.putIfAbsent(idiom.getFirst(), mutableListOf(idiom))?.also {
            it.add(idiom)
        }
        // 添加到末字符map
        endIdiomMap.putIfAbsent(idiom.getEnd(), mutableListOf(idiom))?.also {
            it.add(idiom)
        }
    }

    /**
     * 检查成语列表, 生成未使用的成语列表
     */
    fun check() {
        val unusedIdiomList = mutableListOf<Idiom>()
        endIdiomMap.keys.forEach {
            firstIdiomMap[it]?.ifEmpty { null }.isNull {
                unusedIdiomList.addAll(endIdiomMap[it]!!)
            }
        }
        this.unusedIdiomList = unusedIdiomList
    }

    /**
     * 验证是不是成语
     * 如果是不可使用的成语,则返回空成语
     */
    fun verify(word: String) = idiomWordMap[word]?.let { idiom ->
        unusedIdiomList.find { it.word == idiom.word }?.also {
            return@let Idiom("", "")
        }
        idiom
    }

    /**
     * 根据最后一个拼音获取成语
     */
    fun verifyByEnd(word: String, nowIdiom: Idiom) =
        getIdiomsByIdiomEnd(nowIdiom)?.find { it.word == word }?.let { idiom ->
            unusedIdiomList.find { it.word == idiom.word }?.also {
                return@let Idiom("", "")
            }
            idiom
        }

    /**
     * 获取随机成语
     */
    fun randomIdiom() = idiomList.random()

    private fun getIdiomsByFirst(first: String) = endIdiomMap[first]
    private fun getIdiomsByEnd(end: String) = firstIdiomMap[end]
    private fun getIdiomsByIdiomFirst(idiom: Idiom) = endIdiomMap[idiom.getFirst()]
    private fun getIdiomsByIdiomEnd(idiom: Idiom) = firstIdiomMap[idiom.getEnd()]
}

class Idiom(val word: String, private val originalPinyin: String) {
    @Suppress("SpellCheckingInspection")
    private val pinyinArr: Array<String> =
        originalPinyin.replace(Regex("[āáǎà]"), "a").replace(Regex("[ōóǒò]"), "o").replace(Regex("[ēéěè]"), "e")
            .replace(Regex("[īíǐì]"), "i").replace(Regex("[ūúǔù]"), "u").replace(Regex("[ǖǘǚǜü]"), "v").split(" ")
            .toTypedArray()

    override fun toString(): String {
        return "$word($originalPinyin)"
    }

    fun getFirst() = pinyinArr.first()
    fun getEnd() = pinyinArr.last()
}