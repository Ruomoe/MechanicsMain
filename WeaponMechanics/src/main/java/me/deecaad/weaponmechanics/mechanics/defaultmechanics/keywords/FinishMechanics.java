package me.deecaad.weaponmechanics.mechanics.defaultmechanics.keywords;

import me.deecaad.weaponmechanics.mechanics.IMechanic;
import me.deecaad.weaponmechanics.mechanics.Mechanics;

import java.util.List;

/**
 * For reload
 */
public class FinishMechanics extends Mechanics {

    /**
     * Default constructor for serializer
     */
    public FinishMechanics() {
    }

    public FinishMechanics(List<IMechanic<?>> mechanicList) {
        super(mechanicList);
    }

    @Override
    public String getKeyword() {
        return "Finish_Mechanics";
    }
}