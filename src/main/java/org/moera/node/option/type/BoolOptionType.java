package org.moera.node.option.type;

import java.util.function.Consumer;

import org.moera.node.option.exception.DeserializeOptionValueException;
import org.moera.node.option.exception.UnsuitableOptionValueException;

@OptionType("bool")
public class BoolOptionType extends OptionTypeBase {

    private boolean parse(String value, Consumer<String> invalidValue) {
        if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "1".equals(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value) || "0".equals(value)) {
            return false;
        }
        invalidValue.accept(value);
        return false; // unreachable
    }

    @Override
    public Object deserializeValue(String value) {
        return parse(value, v -> {
            throw new DeserializeOptionValueException(getTypeName(), v);
        });
    }

    @Override
    public Boolean getBool(Object value) {
        return (Boolean) value;
    }

    @Override
    public Integer getInt(Object value) {
        return ((Boolean) value) ? 1 : 0;
    }

    @Override
    public Long getLong(Object value) {
        return ((Boolean) value) ? 1L : 0;
    }

    @Override
    public Object accept(Object value) {
        if (value instanceof Boolean) {
            return value;
        }
        if (value instanceof Integer) {
            return ((Integer) value) != 0;
        }
        if (value instanceof Long) {
            return ((Long) value) != 0;
        }
        if (value instanceof String) {
            return parse((String) value, v -> {
                throw new UnsuitableOptionValueException(v);
            });
        }
        return super.accept(value);
    }

}
