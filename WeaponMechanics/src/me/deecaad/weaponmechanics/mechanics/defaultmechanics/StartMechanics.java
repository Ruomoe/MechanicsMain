package me.deecaad.weaponmechanics.mechanics.defaultmechanics;

import me.deecaad.weaponmechanics.mechanics.IMechanic;
import me.deecaad.weaponmechanics.mechanics.Mechanics;

import java.util.List;

/**
 * For reload
 */
public class StartMechanics extends Mechanics {

    public StartMechanics() { }

    public StartMechanics(List<IMechanic<?>> mechanicList) {
        super(mechanicList);
    }

    @Override
    public String getKeyword() {
        return "Start_Mechanics";
    }
}