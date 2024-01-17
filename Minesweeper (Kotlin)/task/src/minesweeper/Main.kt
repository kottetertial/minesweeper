package minesweeper

import kotlin.random.Random

fun main() {
    play()
}

fun play() {
    val field = setUp()
    battle(field)
}

fun setUp(): Field {
    print("How many mines do you want on the field? ")
    val minesNumber = readln().toInt()

    return Field(minesNumber).also { println(it) }
}

fun battle(field: Field) {
    var isFirstAttempt = true
    do {
        print("Set/unset mine marks or claim a cell as free: ")
        val (x, y, action) = readln().split(" ")
        val guess = FieldCell(y.toInt() - 1, x.toInt() - 1)
        if (action == "free") {
            if (isFirstAttempt) {
                exploreFirst(field, guess)
                isFirstAttempt = false
            } else if (!explore(field, guess)) {
                field.revealMines()
                break
            }
        } else if (action == "mine") field.mark(guess)
        println(field)
    } while (field.isMined())
    if (field.isMined()) {
        println("You stepped on a mine and failed!")
        println(field)
    } else println("Congratulations! You found all the mines!")
}

fun exploreFirst(field: Field, guess: FieldCell) {
    if (field.isMinedAt(guess)) field.moveMine(guess)
    explore(field, guess)
}

fun explore(field: Field, guess: FieldCell): Boolean {
    if (field.isMinedAt(guess)) return false
    field.explore(guess)
    return true
}

data class FieldCell(val row: Int, val column: Int) {
    fun getAdjacentCells(): Sequence<FieldCell> {
        return sequence {
            (row - 1..row + 1).forEach { i ->
                (column - 1..column + 1).forEach { j ->
                    if (!(i == row && j == column)) yield(FieldCell(i, j))
                }
            }
        }
    }
}

class Field(minesNumber: Int, private val width: Int = 9, private val height: Int = 9) {
    private val field: List<MutableList<Char>> = List(height) { MutableList(width) { '.' } }
    private val mines: MutableSet<FieldCell> = mutableSetOf()
    private val hints: MutableMap<FieldCell, Char> = mutableMapOf()
    private var markedCells: Int = 0
    private var locatedMines: Int = 0
    private var revealedCells: Int = 0

    init {
        populateField(minesNumber)
    }

    fun isMinedAt(cell: FieldCell) = cell in mines

    fun moveMine(mine: FieldCell) {
        removeHints(mine)
        val newMine = insertMineExcluding(mine)
        mines.remove(mine)
        insertHints(newMine)
        replaceMineWithHint(mine)
    }

    fun explore(cell: FieldCell) {
        if (isMinedAt(cell) || isExplored(cell)) return
        if (hasHintAt(cell)) {
            if (!hasHintRevealedAt(cell)) {
                revealHint(cell)
                revealedCells++
            }
            return
        }
        set(cell, '/')
        revealedCells++
        cell.getAdjacentCells()
                .filter { has(it) }
                .forEach { explore(it) }
    }

    fun mark(cell: FieldCell) {
        if (isMarkedAt(cell)) removeMark(cell) else insertMark(cell)
    }

    fun revealMines() {
        mines.forEach { set(it, 'X') }
    }

    fun isMined(): Boolean {
        val notAllMinesLocatedAndMarked = locatedMines < mines.size || markedCells != mines.size
        val notEnoughSafeCellsOpen = width * height - revealedCells != mines.size
        return notAllMinesLocatedAndMarked && notEnoughSafeCellsOpen
    }

    override fun toString(): String {
        return buildString {
            append(" |")
            repeat(width) {
                append(it + 1)
            }
            appendLine("|")
            appendLine("—|${"—".repeat(width)}|")
            field.forEachIndexed { index, chars ->
                appendLine("${index + 1}|${chars.joinToString("")}|")
            }
            appendLine("—|${"—".repeat(width)}|")
        }
    }

    private fun populateField(minesNumber: Int) {
        insertMines(minesNumber)
        insertHints()
    }

    private fun insertMines(minesExpected: Int) {
        var minesActual = 0
        while (minesActual < minesExpected) {
            val mine = FieldCell(Random.nextInt(0, height), Random.nextInt(0, width))
            if (insertMine(mine)) minesActual++
        }
    }

    private fun insertMine(cell: FieldCell): Boolean {
        return if (isEmptyAt(cell)) mines.add(cell) else false
    }

    private fun insertMineExcluding(cell: FieldCell): FieldCell {
        var mine = FieldCell(Random.nextInt(0, height), Random.nextInt(0, width))
        if (mine != cell) (insertMineExcluding(cell))
        mines.add(cell)
        return mine
    }

    private fun replaceMineWithHint(mine: FieldCell) {
        val count = mine.getAdjacentCells()
                .filter { has(it) }
                .filter { isMinedAt(it) }
                .count()
        hints[mine] = count.toString().first()
    }

    private fun insertHints() {
        mines.forEach { insertHints(it) }
    }

    private fun insertHints(mine: FieldCell) {
        mine.getAdjacentCells()
                .filter { has(it) }
                .filterNot { isMinedAt(it) }
                .forEach { insertHint(it) }
    }

    private fun insertHint(cell: FieldCell) {
        hints[cell] = if (isEmptyAt(cell)) '1' else getHint(cell) + 1
    }

    private fun removeHints(mine: FieldCell) {
        mine.getAdjacentCells()
                .filter { has(it) }
                .filter { hasHintAt(it) }
                .forEach { removeHint(it) }
    }

    private fun removeHint(cell: FieldCell) {
        val currentHint = getHint(cell)
        if (currentHint == '1') hints.remove(cell)
        else hints[cell] = currentHint - 1
    }

    private fun revealHint(hint: FieldCell) {
        set(hint, getHint(hint))
    }

    private fun getHint(hint: FieldCell) = hints[hint]!!

    private fun removeMark(cell: FieldCell) {
        if (isMinedAt(cell)) locatedMines--
        set(cell, '.')
        markedCells--
    }

    private fun insertMark(cell: FieldCell) {
        if (isMinedAt(cell)) locatedMines++
        set(cell, '*')
        markedCells++
    }

    private fun isExplored(cell: FieldCell) = get(cell) == '/'

    private fun isMarkedAt(cell: FieldCell) = get(cell) == '*'

    private fun hasHintAt(cell: FieldCell) = cell in hints

    private fun hasHintRevealedAt(cell: FieldCell) = get(cell).isDigit()

    private fun isEmptyAt(cell: FieldCell) = !isMinedAt(cell) && !hasHintAt(cell)

    private fun has(cell: FieldCell): Boolean {
        return cell.row in 0 until height && cell.column in 0 until width
    }

    private fun get(cell: FieldCell) = field[cell.row][cell.column]

    private fun set(cell: FieldCell, content: Char) {
        field[cell.row][cell.column] = content
    }
}
