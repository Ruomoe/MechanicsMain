package me.deecaad.weaponmechanics.weapon.trigger;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerEnumException;
import me.deecaad.core.file.SerializerException;
import me.deecaad.weaponmechanics.wrappers.EntityWrapper;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Circumstance implements Serializer<Circumstance> {

    private List<CircumstanceData> circumstances;

    /**
     * Default constructor for serializer
     */
    public Circumstance() {
    }

    public Circumstance(List<CircumstanceData> circumstances) {
        this.circumstances = circumstances;
    }

    public boolean deny(EntityWrapper entityWrapper) {
        for (CircumstanceData circumstance : this.circumstances) {
            if (circumstance.deny(entityWrapper)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    @Override
    public Circumstance serialize(SerializeData data) throws SerializerException {
        ConfigurationSection circumstanceSection = data.config.getConfigurationSection(data.key);
        if (circumstanceSection == null) {
            throw data.exception(null, "Could not find the configuration section of Circumstance");
        }

        List<CircumstanceData> circumstances = new ArrayList<>(1);

        for (String type : circumstanceSection.getKeys(false)) {
            String typeToUpper = type.toUpperCase();

            String value = data.config.getString(data.key + "." + type);
            if (!value.equalsIgnoreCase("DENY") && !value.equalsIgnoreCase("REQUIRED")) {
                throw data.exception(type, "Only DENY and REQUIRED are allowed, now there was " + value + "!");
            }

            try {
                circumstances.add(new CircumstanceData(Type.valueOf(typeToUpper), value.equalsIgnoreCase("REQUIRED")));
            } catch (IllegalArgumentException e) {
                throw new SerializerEnumException(this, Type.class, type, false, data.of().getLocation());
            }
        }

        return new Circumstance(circumstances);
    }

    private static class CircumstanceData {

        private final Type type;
        private final boolean required;

        public CircumstanceData(Type type, boolean required) {
            this.type = type;
            this.required = required;
        }

        public boolean deny(EntityWrapper entityWrapper) {
            return required != switch (type) {
                case RELOADING -> entityWrapper.isReloading();
                case ZOOMING -> entityWrapper.isZooming();
                case SNEAKING -> entityWrapper.isSneaking();
                case STANDING -> entityWrapper.isStanding();
                case WALKING -> entityWrapper.isWalking();
                case RIDING -> entityWrapper.isRiding();
                case SPRINTING -> entityWrapper.isSprinting();
                case DUAL_WIELDING -> entityWrapper.isDualWielding();
                case SWIMMING -> entityWrapper.isSwimming();
                case IN_MIDAIR -> entityWrapper.isInMidair();
                case GLIDING -> entityWrapper.isGliding();
            };
        }
    }

    private enum Type {

        RELOADING,
        ZOOMING,
        SNEAKING,
        STANDING,
        WALKING,
        RIDING,
        SPRINTING,
        DUAL_WIELDING,
        SWIMMING,
        IN_MIDAIR,
        GLIDING
    }
}