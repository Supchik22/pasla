package io.github.supchik22

fun main() {
    val game = Game()
    try {
        game.init()
        game.run()
    } finally {
        game.cleanup()
    }
}