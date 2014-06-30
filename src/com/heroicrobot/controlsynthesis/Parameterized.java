package com.heroicrobot.controlsynthesis;

public interface Parameterized {
  int getNumberOfParameters();
  float getParameter(int paramNum);
  void setParameter(int paramNum, float value);
  String getParameterName(int paramNum);
}
