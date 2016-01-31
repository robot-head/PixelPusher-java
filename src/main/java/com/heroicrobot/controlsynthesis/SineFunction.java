package com.heroicrobot.controlsynthesis;

public class SineFunction implements Receiver, SynthesisModule,
    Transmitter {

  float myInput = 0;
  float myOutput = 0;
  int myChainDepth = 0;
  
  @Override
  public int getNumberOfOutputs() {
    // TODO Auto-generated method stub
    return 1;
  }

  @Override
  public float getOutput(int outputNumber) {
    // TODO Auto-generated method stub
    return myOutput;
  }

  @Override
  public String getOutputName(int outputNumber) {
    return "Sine of x";
  }

  @Override
  public String getName() {
    return "Sine";
  }

  @Override
  public int getChainDepth() {
    return myChainDepth;
  }

  @Override
  public boolean hasInputs() {
    return true;
  }

  @Override
  public boolean hasOutputs() {
    return true;
  }

  @Override
  public void tick() {
   myOutput = (float) Math.sin(myInput);
  }

  @Override
  public int getNumberOfInputs() {
    return 1;
  }

  @Override
  public void setInput(int inputNumber, float value) {
    myInput = value;
  }

  @Override
  public String getInputName(int inputNumber) {
    return "x";
  }

  @Override
  public void setChainDepth(int depth) {
    myChainDepth = depth+1;
  }

}
