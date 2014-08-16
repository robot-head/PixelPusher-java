package com.heroicrobot.controlsynthesis;

public class RampGenerator implements Transmitter, SynthesisModule,
    Parameterized {

  float currentState = 0.0f;
  float initState = 0.0f;
  float increment = 1.0f;
  float maxValue = 255.0f;
  float resetValue = 0.0f;
  
  @Override
  public int getNumberOfParameters() {
    // TODO Auto-generated method stub
    return 4;
  }

  @Override
  public float getParameter(int paramNum) {
    // TODO Auto-generated method stub
    switch (paramNum) {
      case 0:   return initState;
      case 1:   return increment;
      case 2:   return maxValue;
      case 3:   return resetValue;
      default:  return 0.0f;
    }
  }

  @Override
  public void setParameter(int paramNum, float value) {
    switch (paramNum) {
      case 0: initState = value; break;
      case 1: increment = value; break;
      case 2: maxValue = value; break;
      case 3: resetValue = value; break;
      default: return;
    }
  }

  @Override
  public String getParameterName(int paramNum) {
    switch (paramNum) {
      case 0:   return "Initial value";
      case 1:   return "Increment";
      case 2:   return "Maximum value";
      case 3:   return "Reset value";
      default:  return "Unknown parameter";
    }
  }

  @Override
  public boolean hasLimits(int paramNum) {
    return false;   // None of our params have limits
  }

  @Override
  public float getLowerLimit(int paramNum) {
    return 0;
  }

  @Override
  public float getUpperLimit(int paramNum) {
    return 0;
  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return "Ramp generator";
  }

  @Override
  public int getChainDepth() {
    return 0;
  }


  @Override
  public boolean hasOutputs() {
    return true;
  }

  @Override
  public void tick() {
     currentState+=increment;
     if (currentState > maxValue)
        currentState = resetValue;
  }

  @Override
  public int getNumberOfOutputs() {
    // TODO Auto-generated method stub
    return 1;
  }

  @Override
  public float getOutput(int outputNumber) {
    return currentState;
  }

  @Override
  public String getOutputName(int outputNumber) {
    return "Ramp output";
  }

  @Override
  public boolean hasInputs() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void setChainDepth(int depth) {
    // We have no inputs (we are not a Receiver) so it's always zero.
  }
}
