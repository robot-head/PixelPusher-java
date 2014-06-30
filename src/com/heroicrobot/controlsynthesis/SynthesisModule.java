package com.heroicrobot.controlsynthesis;

/*
 *  Pluggable synthesizer module interface.
 */

public interface SynthesisModule {
  String getName();                 // Get the name of this module.
  int getChainDepth();              // How far into the synthesis chain is this module?
  boolean hasInputs();              // Does this module have inputs?
  boolean hasOutputs();             // Does this module have outputs?
  void tick();                      // Cause this module to update its outputs based on current inputs.
}
