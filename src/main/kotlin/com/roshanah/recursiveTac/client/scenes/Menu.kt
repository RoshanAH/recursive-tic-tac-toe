package com.roshanah.recursiveTac.client.scenes

import com.roshanah.recursiveTac.client.rendering.ai
import com.roshanah.recursiveTac.client.rendering.multiplayer
import com.roshanah.recursiveTac.client.rendering.player
import org.openrndr.Program
import org.openrndr.math.Matrix44

fun mainMenu(program: Program) = buildMenu(program, Matrix44.IDENTITY){
    button(3, player){
        textFieldToScene(1){
            Singleplayer(program, this.transform, it.toInt())
        }
    }

    button(4, ai){
        textFieldToMenu(1){
//            Training(program, this.transform, it.toInt())
        }
    }

    button(5, multiplayer){

    }
}