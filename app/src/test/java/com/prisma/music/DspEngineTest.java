package com.prisma.music;
import org.junit.Test;
import static org.junit.Assert.*;

public class DspEngineTest {
    @Test public void disabledEnginePreservesSamplesExactly() {
        DspEngine dsp=new DspEngine(48000,2); float[] pcm={.2f,-.5f,0f,1f}; float[] original=pcm.clone(); dsp.process(pcm); assertArrayEquals(original,pcm,0);
    }
    @Test public void boostsSelectedFrequencyAndLimitsPeak() {
        DspEngine dsp=new DspEngine(48000,1); double[] gains=new double[10]; gains[5]=6;
        dsp.configure(new DspEngine.Config(true,gains,1,0,0,0,0,0));
        float[] wave=new float[48000];for(int i=0;i<wave.length;i++) wave[i]=(float)(.1*Math.sin(2*Math.PI*1000*i/48000));dsp.process(wave);
        double energy=0;for(int i=24000;i<wave.length;i++) energy+=wave[i]*wave[i];double rms=Math.sqrt(energy/24000);
        assertTrue("+6 dB at 1 kHz",rms>.13 && rms<.16);
        float[] loud=new float[48000];for(int i=0;i<loud.length;i++) loud[i]=(float)(.95*Math.sin(2*Math.PI*1000*i/48000));dsp.process(loud);
        for(float sample:loud) assertTrue(Float.isFinite(sample) && Math.abs(sample)<=.967f);
    }
    @Test public void channelsDoNotLeakAndLowSampleRatesRemainStable() {
        DspEngine dsp=new DspEngine(8000,2);double[] gains={12,-12,12,-12,12,-12,12,-12,12,-12};dsp.configure(new DspEngine.Config(true,gains,4,12,12,12,12,12));
        float[] pcm=new float[16000];pcm[0]=1;dsp.process(pcm);
        for(int i=0;i<pcm.length;i++) {assertTrue(Float.isFinite(pcm[i]));assertTrue(Math.abs(pcm[i])<=1);if(i%2==1) assertEquals(0,pcm[i],0);}
    }
}
