package com.bananarepublic.plugin;

import com.bananarepublic.model.resource.ResourceType;

public interface Bank {
    boolean hasResource(ResourceType type, int amount);
    void takeResource(ResourceType type, int amount);
}
