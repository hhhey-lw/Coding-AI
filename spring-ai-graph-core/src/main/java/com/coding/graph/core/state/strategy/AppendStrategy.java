package com.coding.graph.core.state.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AppendStrategy implements KeyStrategy{
    @Override
    public Object apply(Object oldValue, Object newValue) {
        if (oldValue == null){
            return newValue;
        }
        // 如果旧值是Optional，则解包
        if (oldValue instanceof Optional<?> oldValueOptional) {
            oldValue = oldValueOptional.orElse(null);
        }

        // 如果旧值是Optional，则解包
        if (oldValue instanceof Optional<?> oldValueOptional) {
            oldValue = oldValueOptional.orElse(null);
        }

        if (oldValue instanceof List<?> oldListRaw) {
            List<Object> mergedList = new ArrayList<>(oldListRaw);

            if (newValue instanceof List<?> newListRaw) {
                mergedList.addAll(newListRaw);
            } else {
                mergedList.add(newValue);
            }

            return mergedList;
        }
        else {
            ArrayList<Object> newList = new ArrayList<>();
            if (oldValue != null) {
                newList.add(oldValue);
            }
            newList.add(newValue);
            return newList;
        }
    }
}
