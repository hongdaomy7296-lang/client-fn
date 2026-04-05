package com.clientfn.ui;

/**
 * Shared runtime config access interface used by all modules.
 */
public interface IClientConfig {
    boolean isEnabled(String moduleKey);

    float getFloat(String key, float defaultValue);

    void resetToDefaults();
}
