package me.deecaad.core.file;

import me.deecaad.core.utils.EnumUtil;
import me.deecaad.core.utils.ReflectionUtil;
import me.deecaad.core.utils.StringUtil;
import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;

/**
 * {@link SerializeData} wraps a {@link ConfigurationSection} and a key along
 * with useful "validation methods". These methods will throw a
 * {@link SerializerException} if the server admin input an incorrect value.
 * This allows us, the developers, to quickly and easily check if the config is
 * valid (Without long if/else if/else chains, or otherwise). Uses a builder
 * pattern for nice one-liners.
 *
 * <p>For example, to get a positive integer from config, we can use
 * <code>SerializeData#of("your.key").assertExists().assertPositive().getInt()</code>.
 */
public class SerializeData {

    public final String serializer;
    public final File file;
    public final String key;
    public final ConfigurationSection config;

    public SerializeData(@Nonnull String serializer, @Nonnull File file, String key, @Nonnull ConfigurationSection config) {
        this.serializer = serializer;
        this.file = file;
        this.key = key;
        this.config = config;
    }

    public SerializeData(@Nonnull String serializer, @Nonnull SerializeData other, @Nonnull String relative) {
        this.serializer = serializer;
        this.file = other.file;
        this.key = other.getPath(relative);
        this.config = other.config;
    }

    public SerializeData(@Nonnull Serializer<?> serializer, @Nonnull File file, String key, @Nonnull ConfigurationSection config) {
        this.serializer = getSimpleName(serializer);
        this.file = file;
        this.key = key;
        this.config = config;
    }

    public SerializeData(@Nonnull Serializer<?> serializer, @Nonnull SerializeData other, @Nonnull String relative) {
        this.serializer = getSimpleName(serializer);
        this.file = other.file;
        this.key = other.getPath(relative);
        this.config = other.config;
    }

    private static String getSimpleName(Serializer<?> serializer) {

        // Sometimes a class will end with 'Serializer' in its name, like
        // 'ColorSerializer'. This information may be confusing to some people,
        // so we can strip it away here.
        String simple = serializer.getClass().getSimpleName();
        int index = simple.indexOf("Serializer");
        if (index > 0)
            simple = simple.substring(0, index);

        return simple;
    }

    private String getPath(String relative) {
        return key == null || "".equals(key) ? relative : (key + "." + relative);
    }

    /**
     * Helper method to "move" into a new configuration section. The given
     * relative key should <i>always</i> point towards a
     * {@link ConfigurationSection}
     *
     * @param relative The non-null, non-empty key relative to this.key.
     * @return The non-null serialize data.
     * @throws IllegalArgumentException If no configuration section exists at the location.
     */
    public SerializeData move(String relative) {
        return new SerializeData(serializer, this, relative);
    }

    public ConfigListAccessor ofList() {
        String[] split = key.split("\\.");
        StringBuilder key = new StringBuilder();

        for (int i = 0; i < split.length - 1; i++)
            key.append(split[i]).append('.');

        if (key.length() > 0)
            key.setLength(key.length() - 1);

        return new SerializeData(serializer, file, key.toString(), config).ofList(split[split.length - 1]);
    }

    public ConfigAccessor of() {
        String[] split = key.split("\\.");
        StringBuilder key = new StringBuilder();

        for (int i = 0; i < split.length - 1; i++)
            key.append(split[i]).append('.');

        if (key.length() > 0)
            key.setLength(key.length() - 1);

        return new SerializeData(serializer, file, key.toString(), config).of(split[split.length - 1]);
    }

    /**
     * Creates a {@link ConfigAccessor} which accesses the data (stored in
     * config) at <code>this.key + "." + relative</code>. The returned accessor
     * can be used to validate arguments.
     *
     * @param relative The non-null, non-empty key relative to this.key.
     * @return The non-null config accessor.
     */
    public ConfigAccessor of(String relative) {
        return new ConfigAccessor(relative);
    }

    /**
     * Creates a {@link ConfigListAccessor} which accesses the data (stored in
     * config) at <code>this.key + ".' + relative</code>. The returned accessor
     * can be used to validate arguments.
     *
     * @param relative The non-null, non-empty key relative to this.key.
     * @return The non-null config list accessor.
     */
    public ConfigListAccessor ofList(String relative) {
        return new ConfigListAccessor(relative);
    }

    /**
     * Returns <code>true</code> if the given relative config key exists.
     * Otherwise, this method will return false. Usually, you should use
     * {@link ConfigAccessor#assertExists()}.
     *
     * @param relative The non-null relative key.
     * @return <code>true</code> if the key exists.
     */
    public boolean has(String relative) {
        return config.contains(getPath(relative));
    }

    /**
     * When there is no method in {@link ConfigAccessor} to match a specific
     * configuration error, you may check for it manually and use this method
     * to throw a "general" exception.
     *
     * <p>Make sure to keep messages clear and concise. There is no limit to
     * how many messages you may give to the player, but make sure that each
     * message is <i>important</i> and contains <i>useful</i> information.
     *
     * @param relative The nullable relative key.
     * @param messages The non-empty list of messages to include.
     * @return The non-null constructed exception.
     */
    public SerializerException exception(String relative, String... messages) {
        if (messages.length == 0)
            throw new IllegalArgumentException("Hey you! Yeah you! Don't be lazy, add messages!");

        String key = this.key;
        if (relative != null && !relative.isEmpty())
            key = getPath(relative);

        return new SerializerException(serializer, messages, StringUtil.foundAt(file, key));
    }

    /**
     * When there is no method in {@link ConfigListAccessor} to match a
     * specific configuration error, you may check for it manually and use this
     * method to throw a "general" exception.
     *
     * @param relative The nullable relative key.
     * @param index The index (NOT index + 1) of the element that had the error.
     * @param messages The non-empty list of messages to include
     * @return The non-null constructed exception.
     */
    public SerializerException listException(String relative, int index, String... messages) {
        if (messages.length == 0)
            throw new IllegalArgumentException("Hey you! Yeah you! Don't be lazy, add messages!");

        String key = this.key;
        if (relative != null && !relative.isEmpty())
            key = getPath(relative);

        return new SerializerException(serializer, messages, StringUtil.foundAt(file, key, index + 1));
    }


    /**
     * Wraps a configuration KEY (which points to a list of values) to some
     * helper functions to facilitate data serialization. The
     */
    public class ConfigListAccessor {

        // Stores the class arguments, which is used to check the format
        private final LinkedList<ClassArgument> arguments;
        private final String relative;
        private boolean didAssertions; // turns to true after assertList()

        public ConfigListAccessor(String relative) {
            this.arguments = new LinkedList<>();
            this.relative = relative;
        }

        public ConfigListAccessor addArgument(Class<?> clazz, boolean required) {
            return this.addArgument(clazz, required, false);
        }

        public ConfigListAccessor addArgument(Class<?> clazz, boolean required, boolean skipCheck) {

            // Ensure that all required arguments are in order. For example,
            // true~true~false is fine, but true~false~true is impossible to
            // serialize.
            if (required && !arguments.isEmpty() && !arguments.getLast().required)
                throw new IllegalArgumentException("Required arguments must be consecutive");

            ClassArgument arg = new ClassArgument();
            arg.clazz = clazz;
            arg.required = required;
            arg.skipCheck = skipCheck;
            arguments.add(arg);
            return this;
        }

        public ConfigListAccessor assertArgumentPositive() {
            arguments.getLast().positive = true;
            return this;
        }

        public ConfigListAccessor assertArgumentRange(double min, double max) {
            arguments.getLast().min = min;
            arguments.getLast().max = max;
            return this;
        }

        /**
         * Asserts that this key exists in the configuration. This method
         * ensures that the user explicitly defined a value for the key.
         *
         * @return A non-null reference to this accessor (builder pattern).
         * @throws SerializerException If the key is not explicitly defined.
         */
        @Nonnull
        public ConfigListAccessor assertExists() throws SerializerException {
            if (!config.contains(getPath(relative), true))
                throw new SerializerMissingKeyException(serializer, relative, getLocation());

            return this;
        }

        /**
         * If the <code>exists = true</code>, then this method will call
         * {@link #assertExists()}. This is useful for when an argument is only
         * required when another argument is present.
         *
         * @param exists true to assert if the argument exists.
         * @return A non-null reference to this accessor (builder pattern).
         * @throws SerializerException If the key is not explicitly defined.
         */
        public ConfigListAccessor assertExists(boolean exists) throws SerializerException {
            if (exists)
                return assertExists();
            else
                return this;
        }

        @Nonnull
        @SuppressWarnings({"unchecked", "rawtypes"})
        public ConfigListAccessor assertList() throws SerializerException {
            if (arguments.isEmpty())
                throw new IllegalStateException("Need to set arguments before assertions");

            didAssertions = true;

            // The first step is to assert that the value stored at this key
            // is a list (of any generic-type).
            Object value = config.get(getPath(relative));
            if (value == null)
                return this;

            if (!(value instanceof List))
                throw new SerializerTypeException(serializer, List.class, value.getClass(), value, getLocation());
            List<?> list = (List<?>) config.get(getPath(relative));

            // Use assertExists for required keys
            if (list == null || list.isEmpty())
                return this;

            for (int i = 0; i < list.size(); i++) {
                String string = Objects.toString(list.get(i));

                // Show the user the correct format
                StringBuilder format = new StringBuilder("<");
                arguments.forEach(arg -> {
                    format.append(arg.clazz.getSimpleName());
                    if (arg.required) format.append('*');
                    format.append(">-<");
                });
                format.append('>');

                // Empty string in config is probably a mistake (Perhaps they
                // forgot to save?). Instead of ignoring this, we should tell
                // the user (playing it safe).
                if (string == null || string.trim().isEmpty()) {
                    throw listException(relative, i, relative + " does not allow empty elements in the list.",
                            "Valid Format: " + format);
                }

                // Each element in the list should be a string of values
                // separated by a standard delimiter (Either '~' or '-' or ' ')
                String[] split = StringUtil.split(string);

                // Missing required data
                int required = (int) arguments.stream().filter(arg -> arg.required).count();
                if (split.length < required) {
                    throw listException(relative, i, relative + " requires the first " + required + " arguments to be defined.",
                            SerializerException.forValue(string),
                            "You are missing " + (required - split.length) + " arguments",
                            "Valid Format: " + format
                    );
                }

                for (int j = 0; j < split.length; j++) {

                    // Extra data check. This happens when the user adds more
                    // data than what the list can take. For example, if this
                    // list uses the format 'string-int' and the user inputs
                    // 'string-int-double', then this will be triggered.
                    if (arguments.size() <= j) {
                        throw listException(relative, i, "Invalid list format, " + relative + " can only use " + arguments.size() + " arguments.",
                                SerializerException.forValue(string),
                                "Valid Format: " + format
                        );
                    }

                    String component = split[j];
                    ClassArgument argument = arguments.get(j);
                    if (argument.skipCheck)
                        continue;

                    try {
                        if (argument.clazz == int.class) {
                            argument.clazz = Integer.class; // Set class to be more human-readable in error
                            int parseInt = Integer.parseInt(component);
                            if (!Double.isNaN(argument.min) && !Double.isNaN(argument.max) && (parseInt < argument.min || parseInt > argument.max))
                                throw new SerializerRangeException(serializer, (int) argument.min, parseInt, (int) argument.max, getLocation(i))
                                        .addMessage("Full List Element: " + string)
                                        .addMessage("Valid List Format: " + format);

                            if (argument.positive && parseInt < 0)
                                throw new SerializerNegativeException(serializer, parseInt, getLocation(i))
                                        .addMessage("Full List Element: " + string)
                                        .addMessage("Valid List Format: " + format);
                        }

                        else if (argument.clazz == double.class) {
                            argument.clazz = Double.class;
                            double parseDouble = Double.parseDouble(component);
                            if (!Double.isNaN(argument.min) && !Double.isNaN(argument.max) && (parseDouble < argument.min || parseDouble > argument.max))
                                throw new SerializerRangeException(serializer, argument.min, parseDouble, argument.max, getLocation(i))
                                        .addMessage("Full List Element: " + string)
                                        .addMessage("Valid List Format: " + format);

                            if (argument.positive && parseDouble < 0.0)
                                throw new SerializerNegativeException(serializer, parseDouble, getLocation(i))
                                        .addMessage("Full List Element: " + string)
                                        .addMessage("Valid List Format: " + format);

                        } else if (argument.clazz == boolean.class) {
                            argument.clazz = Boolean.class;
                            if (!component.equalsIgnoreCase("true") && !component.equalsIgnoreCase("false"))
                                throw new Exception();

                        } else if (argument.clazz.isEnum() && EnumUtil.parseEnums((Class<Enum>) argument.clazz, component).isEmpty()) {
                            throw new SerializerEnumException(serializer, (Class<Enum>) argument.clazz, component, true, getLocation(i))
                                    .addMessage("Full List Element: " + string)
                                    .addMessage("Valid List Format: " + format);
                        }
                    } catch (SerializerException ex) {
                        throw ex; // Rethrow exception so it isn't caught and ignored
                    } catch (Exception ex) {
                        throw new SerializerTypeException(serializer, argument.clazz, null, component, getLocation(i))
                                .addMessage("Full List Element: " + string)
                                .addMessage("Valid List Format: " + format);
                    }
                }
            }

            return this;
        }

        @SuppressWarnings("rawtypes")
        public List<String[]> get() {
            if (!didAssertions)
                throw new IllegalStateException("Forgot to call assertList()? Did something go wrong?");

            // Use assertExists for required keys
            if (!config.contains(getPath(relative)))
                return Collections.emptyList();

            List<String[]> list = new ArrayList<>();
            for (Object obj : (List) config.get(getPath(relative), Collections.emptyList())) {
                list.add(StringUtil.split(obj.toString()));
            }

            return list;
        }

        public String getLocation() {
            if (relative == null || "".equals(relative)) {
                return StringUtil.foundAt(file, key);
            } else {
                return StringUtil.foundAt(file, getPath(relative));
            }
        }

        public String getLocation(int index) {
            if (relative == null || "".equals(relative)) {
                return StringUtil.foundAt(file, key, index + 1);
            } else {
                return StringUtil.foundAt(file, getPath(relative), index + 1);
            }
        }

        private class ClassArgument {
            Class<?> clazz;
            boolean required;
            boolean skipCheck;

            boolean positive;
            double min = Double.NaN;
            double max = Double.NaN;
        }
    }


    /**
     * Wraps a configuration KEY to some helper functions to facilitate data
     * serialization. The (public) methods of this class will throw a
     * {@link SerializerException} if the configuration is invalid.
     *
     * <p>The methods of this class follow the Builder pattern.
     */
    public class ConfigAccessor {

        protected final String relative;
        private boolean exists;

        private ConfigAccessor(String relative) {
            this.relative = relative;
        }

        /**
         * Asserts that this key exists in the configuration. This method
         * ensures that the user explicitly defined a value for the key.
         *
         * @return A non-null reference to this accessor (builder pattern).
         * @throws SerializerException If the key is not explicitly defined.
         */
        @Nonnull
        public ConfigAccessor assertExists() throws SerializerException {
            if (!config.contains(getPath(relative), true))
                throw new SerializerMissingKeyException(serializer, relative, getLocation());

            exists = true;
            return this;
        }

        /**
         * If the <code>exists = true</code>, then this method will call
         * {@link #assertExists()}. This is useful for when an argument is only
         * required when another argument is present.
         *
         * @param exists true to assert if the argument exists.
         * @return A non-null reference to this accessor (builder pattern).
         * @throws SerializerException If the key is not explicitly defined.
         */
        public ConfigAccessor assertExists(boolean exists) throws SerializerException {
            if (exists)
                return assertExists();
            else
                return this;
        }

        /**
         * Returns <code>true</code> when the object stored in this location
         * matches the given <code>type</code>.
         *
         * @param type Which type to check for
         * @return true, if the value matched the type.
         */
        public boolean is(Class<?> type) {
            Object value = config.get(getPath(relative));
            return value != null && type.isAssignableFrom(value.getClass());
        }

        /**
         * Asserts that the value at this key is an instance of the given
         * class. Ensures that the datatype matches what the developer
         * expected the user to give.
         *
         * @param type The non-null data type to match.
         * @return A non-null reference to this accessor (builder pattern).
         * @throws SerializerException If the type does not match.
         */
        @Nonnull
        public ConfigAccessor assertType(Class<?> type) throws SerializerException {
            Object value = config.get(getPath(relative));

            // Use assertExists for required keys
            if (value != null) {
                Class<?> actual = value.getClass();
                if (!type.isAssignableFrom(actual)) {
                    throw new SerializerTypeException(serializer, type, actual, value, getLocation());
                }
            }

            return this;
        }

        /**
         * Returns the integer value of the config, or throws an exception if
         * the value is not a number. Note that this method will also throw an
         * exception if the input is explicitly a double. For example,
         * <code>1.0</code> is a valid integer which will be parsed as
         * <code>1</code>, but <code>1.1</code> will throw an exception.
         *
         * <p>Note that this method should only be called if you have already
         * used {@link #assertExists()}. For non-required values, instead use
         * {@link #getInt(int)}.
         *
         * @return The integer from config.
         * @throws SerializerException If the config value is not an integer.
         */
        public int getInt() throws SerializerException {
            if (!exists)
                throw new IllegalStateException("Either provide a default value or use assertExists()!");

            return getInt(Integer.MIN_VALUE);
        }

        /**
         * Returns the integer value of the config, or throws an exception if
         * the value is not a number. Note that this method will also throw an
         * exception if the input is explicitly a double. For example,
         * <code>1.0</code> is a valid integer which will be parsed as
         * <code>1</code>, but <code>1.1</code> will throw an exception.
         *
         * @return The integer from config.
         * @param def The default value to return when the config is undefined.
         * @throws SerializerException If the config value is not an integer.
         */
        public int getInt(int def) throws SerializerException {
            Number num = getNumber(def);
            if (Double.compare(Math.floor(num.doubleValue()), Math.ceil(num.doubleValue())) != 0)
                throw new SerializerTypeException(serializer, Integer.class, Double.class, num, getLocation());

            return num.intValue();
        }

        /**
         * Returns the double value of the config, or throws an exception if
         * the value is not a number.
         *
         * <p>Note that this method should only be called if you have already
         * used {@link #assertExists()}. For non-required values, instead use
         * {@link #getDouble(double)}.
         *
         * @return The integer from config.
         * @throws SerializerException If the config value is not a double.
         */
        public double getDouble() throws SerializerException {
            if (!exists)
                throw new IllegalArgumentException("Either provide a default value or use assertExists()!");

            return getDouble(Double.NaN);
        }

        /**
         * Returns the double value of the config, or throws an exception if
         * the value is not a number.
         *
         * @return The double from config.
         * @param def The default value to return when the config is undefined.
         * @throws SerializerException If the config value is not a double.
         */
        public double getDouble(double def) throws SerializerException {
            return getNumber(def).doubleValue();
        }

        /**
         * Returns the boolean value of the config, or throws an exception if
         * the value is not a boolean.
         *
         * <p>Note that this method should only be called if you have already
         * used {@link #assertExists()}. For non-required values, instead use
         * {@link #getBool(boolean)}.
         *
         * @return The boolean from config.
         * @throws SerializerException If the config value is not a boolean.
         */
        public boolean getBool() throws SerializerException {
            if (!exists)
                throw new IllegalStateException("Either provide a default value or use assertExists()!");

            return getBool(false);
        }

        /**
         * Returns the boolean value of the config, or throws an exception if
         * the value is not a boolean.
         *
         * @return The boolean from config.
         * @param def The default value to return when the config is undefined.
         * @throws SerializerException If the config value is not a boolean.
         */
        public boolean getBool(boolean def) throws SerializerException {
            Object value = config.get(getPath(relative));
            if (value == null)
                return def;

            if (value instanceof Boolean)
                return (Boolean) value;
            if (value instanceof String) {
                if (value.toString().trim().equalsIgnoreCase("true"))
                    return true;
                if (value.toString().trim().equalsIgnoreCase("false"))
                    return false;
            }

            throw new SerializerTypeException(serializer, Boolean.class, value.getClass(), value, getLocation());
        }

        /**
         * Asserts that the value at this key is a number of any type. The
         * check is done by checking the value can be type-casted to a double.
         * Note that if you want a more specific number type (for example, an
         * integer), you should use {@link #assertType(Class)}.
         *
         * @return A non-null reference to this accessor (builder pattern).
         * @throws SerializerException If the type is not a number.
         */
        public Number getNumber(Number def) throws SerializerException {
            Object value = config.get(getPath(relative));

            // Use assertExists for required keys
            if (value == null)
                return def;

            try {
                return (Number) value;
            } catch (ClassCastException ex) {
                throw new SerializerTypeException(serializer, Number.class, value.getClass(), value, getLocation());
            }
        }

        /**
         * Asserts that the value at this key is a number, AND that the number
         * is positive. Note that if you want a more specific number type (for
         * example, an integer), you should use {@link #getInt(int)}.
         *
         * @return A non-null reference to this accessor (builder pattern).
         * @throws SerializerException If the type is not a number or is not positive.
         */
        @Nonnull
        public ConfigAccessor assertPositive() throws SerializerException {
            Number value = getNumber(null);

            // Use assertExists for required keys
            if (value != null) {

                if ((value instanceof Long && value.longValue() < 0L) || value.doubleValue() < 0L)
                    throw new SerializerNegativeException(serializer, value, getLocation());
            }

            return this;
        }

        /**
         * Asserts that the value at this key is a number, AND that the number
         * is within the inclusive range. Note that if you want a more specific
         * number type (for example, an integer), you should use
         * {@link #getInt(int)}.
         *
         * @param min Inclusive minimum bound.
         * @param max Inclusive maximum bound.
         * @return A non-null reference to this accessor (builder pattern).
         * @throws SerializerException If the value is not in range.
         * @throws IllegalArgumentException If min larger than max.
         */
        @Nonnull
        public ConfigAccessor assertRange(int min, int max) throws SerializerException {
            if (min > max)
                throw new IllegalArgumentException("min > max");

            Number value = getNumber(null);

            // Use assertExists for required keys
            if (value != null) {

                // Silently strips away float point data (without exception)
                int num = value.intValue();
                if (num < min || num > max)
                    throw new SerializerRangeException(serializer, min, num, max, getLocation());
            }

            return this;
        }

        /**
         * Asserts that the value at this key is a number, AND that the number
         * is within the inclusive range. Note that if you want a more specific
         * number type (for example, an integer), you should use
         * {@link #getInt(int)}.
         *
         * @param min Inclusive minimum bound.
         * @param max Inclusive maximum bound.
         * @return A non-null reference to this accessor (builder pattern).
         * @throws SerializerException If the value is not in range.
         * @throws IllegalArgumentException If min larger than max.
         */
        @Nonnull
        public ConfigAccessor assertRange(double min, double max) throws SerializerException {
            if (min > max)
                throw new IllegalArgumentException("min > max");

            Number value = getNumber(null);

            // Use assertExists for required keys
            if (value != null) {

                double num = value.doubleValue();
                if (num < min || num > max)
                    throw new SerializerRangeException(serializer, min, num, max, getLocation());
            }

            return this;
        }

        public String getLocation() {
            if (relative == null || "".equals(relative)) {
                return StringUtil.foundAt(file, key);
            } else {
                return StringUtil.foundAt(file, getPath(relative));
            }
        }

        /**
         * Gets the data stored at this relative key. Note that this method
         * (basically) requires a previous call to {@link #assertExists()},
         * especially for primitive types. When the key is optional, use
         * {@link #get(Object)} to define a default value.
         *
         * @param <T> The expected data-type of the data.
         * @return The data stored at this relative key.
         */
        @SuppressWarnings("unchecked")
        public <T> T get() {
            if (!exists)
                throw new IllegalStateException("Either provide a default value or use assertExists()!");

            return (T) config.get(getPath(relative));
        }

        /**
         * Gets the data stored at this relative key, or
         * <code>defaultValue</code> if the key is not explicitly defined. It
         * does not make sense to use this method when there has been a
         * previous call to {@link #assertExists()}.
         *
         * @param defaultValue The default value to return when one has not been defined.
         * @param <T> The expected data-type of the data.
         * @return The data stored at this relative key, or default.
         */
        @SuppressWarnings("unchecked")
        public <T> T get(T defaultValue) {
            return (T) config.get(getPath(relative), defaultValue);
        }

        /**
         * Shorthand for using {@link #getEnum(Class, Enum)} and returning
         * <code>null</code> by default.
         *
         * @param clazz The non-null enum class that is expected.
         * @param <T> The enum type.
         * @return The user input enum value, or null.
         * @throws SerializerException If the user defined an invalid type.
         */
        public <T extends Enum<T>> T getEnum(@Nonnull Class<T> clazz) throws SerializerException {
            if (!exists)
                throw new IllegalStateException("Either provide a default value or use assertExists()!");

            return getEnum(clazz, null);
        }

        /**
         * Serializes an enum value from config. If the key is not defined,
         * then <code>defaultValue</code> is returned. If the user defines a
         * string that doesn't match any enum, a {@link SerializerEnumException}
         * is thrown.
         *
         * @param clazz The non-null enum class.
         * @param defaultValue The default value to use when a key is undefined.
         * @param <T> The enum type.
         * @return The serialized enum type, or defaultValue.
         * @throws SerializerException If there is a misconfiguration in config.
         */
        public <T extends Enum<T>> T getEnum(@Nonnull Class<T> clazz, T defaultValue) throws SerializerException {
            String input = config.get(getPath(relative), "").toString().trim();

            // Use assertExists for required keys
            if (input.isEmpty())
                return defaultValue;

            // Wildcards are not allowed for singleton enums, they are only
            // allowed for lists.
            if (input.startsWith("$"))
                throw new SerializerEnumException(serializer, clazz, input, false, getLocation());

            // The returned value will have either 0 elements (meaning that the
            // input is invalid) OR 1 element (meaning that the input is valid).
            List<T> list = EnumUtil.parseEnums(clazz, input);
            if (list.isEmpty()) {
                throw new SerializerEnumException(serializer, clazz, input, false, getLocation());
            }

            // At this point, the list is guaranteed to have exactly 1 element.
            return list.get(0);
        }

        /**
         * Returns the string value of the config, adjusted to fit the
         * adventure format. Adventure text is formatting using html-like tags
         * instead of the legacy <code>{@literal &}</code> symbol. If the string in config
         * contains the legacy color system, we will attempt to convert it.
         *
         * <p>The returned string should be parsed using
         * {@link net.kyori.adventure.text.minimessage.MiniMessage}. You may
         * use MechanicsCore's instance {@link me.deecaad.core.MechanicsCore#message}.
         *
         * <p>Note that this method should only be called if you have already
         * used {@link #assertExists()}. For non-required values, instead use
         * {@link #getAdventure(String)}.
         *
         * @return The converted string from config.
         */
        public String getAdventure() {
            if (!exists)
                throw new IllegalStateException("Either provide a default value or use assertExists()!");

            return getAdventure(null);
        }

        /**
         * Returns the string value of the config, adjusted to fit the
         * adventure format. Adventure text is formatting using html-like tags
         * instead of the legacy <code>{@literal &}</code> symbol. If the string in config
         * contains the legacy color system, we will attempt to convert it.
         *
         * <p>The returned string should be parsed using
         * {@link net.kyori.adventure.text.minimessage.MiniMessage}. You may
         * use MechanicsCore's instance {@link me.deecaad.core.MechanicsCore#message}.
         *
         * @return The converted string from config.
         */
        public String getAdventure(String defaultValue) {
            if (!config.contains(getPath(relative)))
                return defaultValue;

            String value = config.getString(getPath(relative));
            assert value != null;

            return StringUtil.colorAdventure(value);
        }

        /**
         * Handles nested serializers. Uses the given class as a serializer and
         * attempts to serialize an object from this relative key. Returns null
         * when the key hasn't been explicitly defined.
         *
         * @param serializerClass The non-null serializer class.
         * @param <T> The serializer type.
         * @return The serialized object.
         * @throws SerializerException If there is a mistake in config found during serialization.
         */
        public <T extends Serializer<T>> T serialize(@Nonnull Class<T> serializerClass) throws SerializerException {
            return serialize(ReflectionUtil.newInstance(serializerClass));
        }

        /**
         * Handles nested serializers. Uses the given serializer to serialize
         * an object from this relative key. Returns null when the key hasn't
         * been explicitly defined.
         *
         * @param serializer The non-null serializer instance.
         * @param <T> The serializer type.
         * @return The serialized object.
         * @throws SerializerException If there is a mistake in config found during serialization.
         */
        public <T extends Serializer<T>> T serialize(@Nonnull T serializer) throws SerializerException {

            // Use assertExists for required keys
            if (!config.contains(getPath(relative)))
                return null;

            SerializeData data = new SerializeData(serializer, SerializeData.this, relative);
            return serializer.serialize(data);
        }

        /**
         * Usually, all serializers will instantiate an instance of themselves:
         *
         * <blockquote><pre>{@code
         * public class Sound extends Serializer<Sound> {
         *     // missing implementation
         * }
         * }</pre></blockquote>
         *
         * But sometimes, a serializer has no choice but to serialize some
         * other class:
         *
         * <blockquote><pre>{@code
         * public class ColorSerializer extends Serializer<Color> {
         *     // missing implementation
         * }
         * }</pre></blockquote>
         *
         * For these situations, this method must be used instead of
         * {@link #serialize(Serializer)} in order to avoid a compile-time
         * error.
         *
         * @param serializer The non-null serializer to use.
         * @param <T> The serialized type.
         * @return The serialized object.
         * @throws SerializerException If there was an error in config.
         */
        public <T> T serializeNonStandardSerializer(@Nonnull Serializer<T> serializer) throws SerializerException {
            // Use assertExists for required keys
            if (!config.contains(getPath(relative)))
                return null;

            SerializeData data = new SerializeData(serializer, SerializeData.this, relative);
            return serializer.serialize(data);
        }
    }
}
