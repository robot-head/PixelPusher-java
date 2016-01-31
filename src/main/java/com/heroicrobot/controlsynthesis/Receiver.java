package com.heroicrobot.controlsynthesis;

/*
 * Pluggable synthesis module with inputs (ie., receives data).
 */

public interface Receiver extends SynthesisModule {
  int getNumberOfInputs();                          // How many inputs do we have?
  void setInput(int inputNumber, float value);      // Set internal state based on this input
  String getInputName(int inputNumber);             // Get the name of this input.
}
