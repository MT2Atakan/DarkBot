package com.github.manolo8.darkbot.extensions.features.handlers;

import com.github.manolo8.darkbot.extensions.features.FeatureDefinition;
import com.github.manolo8.darkbot.utils.ReflectionUtils;

import java.lang.reflect.Type;
import java.util.stream.Stream;

public abstract class FeatureHandler<T> {

    private final Class<T> handledType;

    public FeatureHandler() {
        Type[] types = ReflectionUtils.findGenericParameters(getClass(), FeatureHandler.class);
        if (types == null)
            throw new UnsupportedOperationException("Can't initialize feature decorator with no found type: " + getClass().getCanonicalName());

        //noinspection unchecked
        handledType = (Class<T>) types[0];
    }

    public Class<T> getHandledType() {
        return handledType;
    }

    public abstract Class<?>[] getNativeFeatures();
    public abstract void update(Stream<FeatureDefinition<T>> features);

}
