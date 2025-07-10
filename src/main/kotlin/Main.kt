package io.github.supchik22


val game = Game()


fun main() {

    try {
        game.init()
        game.run()
    } finally {
        game.cleanup()
    }
}