package com.heroicrobot.controlsynthesis;

public class ConstantGenerator implements SynthesisModule, Transmitter, Parameterized {

  float myValue = 0.0f;
  
  @Override
  public int getNumberOfOutputs() {
    return 1;
  }

  @Override
  public float getOutput(int outputNumber) {
    // TODO Auto-generated method stub
    return myValue;
  }

  @Override
  public String getOutputName(int outputNumber) {
    return "Constant output";
  }

  @Override
  public String getName() {
    return "Constant generator";
  }

  @Override
  public int getChainDepth() {
    return 0;       // always zero for ConstantGenerator
  }

  @Override
  public boolean hasInputs() {
    return false;
  }

  @Override
  public boolean hasOutputs() {
    return true;
  }

  @Override
  public void tick() {
    // This is a null op for ConstantGenerator.
  }

  @Override
  public int getNumberOfParameters() {
    return 1;
  }

  @Override
  public float getParameter(int paramNum) {
    return myValue;
  }

  @Override
  public void setParameter(int paramNum, float value) {
    myValue = value;
  }

  @Override
  public String getParameterName(int paramNum) {
    return "Constant value";
  }

  @Override
  public boolean hasLimits(int paramNum) {
    return false;     // this module can generate any float
  }

  @Override
  public float getLowerLimit(int paramNum) {
    return 0;         // meaningless because no limits
  }

  @Override
  public float getUpperLimit(int paramNum) {
    return 0;         // meaningless because no limits
  }

  @Override
  public void setChainDepth(int depth) {
    // It's always zero for a constant generator
  }

}
