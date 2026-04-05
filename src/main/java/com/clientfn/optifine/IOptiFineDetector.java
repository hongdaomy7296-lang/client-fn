package com.clientfn.optifine;

/**
 * Runtime detector for OptiFine presence and compatibility path switching.
 */
public interface IOptiFineDetector {

    boolean isOptiFinePresent();

    boolean shouldUseCompatPath();
}
