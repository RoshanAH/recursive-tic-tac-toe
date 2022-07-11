package com.roshanah.rt3.client.scenes

import com.roshanah.rt3.client.rendering.ai
import com.roshanah.rt3.client.rendering.multiplayer
import com.roshanah.rt3.client.rendering.player
import org.openrndr.Program
import org.openrndr.math.Matrix44

fun mainMenu(program: Program) = buildMenu(program, Matrix44.IDENTITY){
    button(3, player){
        textFieldToScene(1){
            Singleplayer(program, this.transform, it.toInt())
        }
    }

    button(4, ai){
        textFieldToScene(1){
            Training(program, this.transform, it.toInt())
        }
    }

    button(5, multiplayer){

    }
}