package com.heroicrobot.controlsynthesis;

/*
 * Interface used by UI software to enable the user to configure synthesis blocks' internal state.
 */

public interface Parameterized {
  int getNumberOfParameters();                  // how many parameters this module has
  float getParameter(int paramNum);             // get the value of a parameter
  void setParameter(int paramNum, float value); // set the value of a parameter
  String getParameterName(int paramNum);        // get the name of a parameter
  boolean hasLimits(int paramNum);              // true if the value has bounds
  float getLowerLimit(int paramNum);
  float getUpperLimit(int paramNum);
}
