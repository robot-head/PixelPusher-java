package com.heroicrobot.controlsynthesis;

/*
 * Pluggable synthesis module with outputs (ie, generates data).
 */

public interface Transmitter extends SynthesisModule {
  int getNumberOfOutputs();                 // How many outputs do we have?
  float getOutput(int outputNumber);        // Get the value of an output
  String getOutputName(int outputNumber);   // Get the name of an output
}
