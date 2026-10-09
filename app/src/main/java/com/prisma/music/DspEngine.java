package com.prisma.music;

/** Original PCM DSP. Ten peaking bands, tone shelves, voice presence and peak limiter. */
public final class DspEngine {
    public static final double[] FREQUENCIES = {31.25,62.5,125,250,500,1000,2000,4000,8000,16000};
    public static final class Config {
        public final boolean enabled;
        public final double[] gains;
        public final double q, preamp, bass, mids, treble, voice;
        public Config(boolean enabled, double[] gains, double q, double preamp, double bass, double mids, double treble, double voice) {
            this.enabled=enabled; this.gains=gains.clone(); this.q=Math.max(.3,Math.min(4,q));
            this.preamp=bounded(preamp); this.bass=bounded(bass); this.mids=bounded(mids); this.treble=bounded(treble); this.voice=bounded(voice);
        }
        private static double bounded(double v) { return Double.isFinite(v)?Math.max(-12,Math.min(12,v)):0; }
        public static Config flat() { return new Config(false,new double[10],1,0,0,0,0,0); }
    }
    private static final class Filter {
        double b0=1,b1,b2,a1,a2;
        final double[] z1,z2;
        Filter(int channels) { z1=new double[channels]; z2=new double[channels]; }
        double sample(double x,int c) { double y=b0*x+z1[c]; z1[c]=b1*x-a1*y+z2[c]; z2[c]=b2*x-a2*y; return y; }
        void peak(double frequency,double gain,double q,double rate) {
            double w=2*Math.PI*Math.min(frequency,rate*.45)/rate, a=Math.pow(10,gain/40), alpha=Math.sin(w)/(2*q), c=Math.cos(w), d=1+alpha/a;
            b0=(1+alpha*a)/d; b1=-2*c/d; b2=(1-alpha*a)/d; a1=-2*c/d; a2=(1-alpha/a)/d;
        }
        void shelf(double frequency,double gain,double rate,boolean high) {
            double w=2*Math.PI*Math.min(frequency,rate*.45)/rate, a=Math.pow(10,gain/40), c=Math.cos(w), s=2*Math.sqrt(a)*Math.sin(w)/Math.sqrt(2);
            double d;
            if(high) { d=(a+1)-(a-1)*c+s; b0=a*((a+1)+(a-1)*c+s)/d; b1=-2*a*((a-1)+(a+1)*c)/d; b2=a*((a+1)+(a-1)*c-s)/d; a1=2*((a-1)-(a+1)*c)/d; a2=((a+1)-(a-1)*c-s)/d; }
            else { d=(a+1)+(a-1)*c+s; b0=a*((a+1)-(a-1)*c+s)/d; b1=2*a*((a-1)-(a+1)*c)/d; b2=a*((a+1)-(a-1)*c-s)/d; a1=-2*((a-1)+(a+1)*c)/d; a2=((a+1)+(a-1)*c-s)/d; }
        }
    }
    private final Filter[] filters=new Filter[14];
    private final int channels, rate;
    private double limiter=1, volume=1;
    private Config config=Config.flat();
    public DspEngine(int rate,int channels) {
        if(rate<=0 || channels<=0) throw new IllegalArgumentException("Invalid PCM format");
        this.rate=rate; this.channels=channels;
        for(int i=0;i<filters.length;i++) filters[i]=new Filter(channels);
    }
    public void configure(Config c) {
        config=c;
        for(int i=0;i<10;i++) filters[i].peak(FREQUENCIES[i], i<c.gains.length?Config.bounded(c.gains[i]):0,c.q,rate);
        filters[10].shelf(130,c.bass,rate,false);
        filters[11].peak(800,c.mids,.7,rate);
        filters[12].shelf(6000,c.treble,rate,true);
        filters[13].peak(2500,c.voice,.8,rate);
    }
    /** Interleaved PCM floats in [-1,1]. Enabled DSP uses a linked-channel limiter. */
    public void process(float[] pcm) {
        if(!config.enabled) return;
        double target=Math.pow(10,config.preamp/20), release=Math.exp(-1.0/(rate*.1)), smoothing=Math.exp(-1.0/(rate*.02));
        for(int frame=0;frame+channels<=pcm.length;frame+=channels) {
            volume=target+(volume-target)*smoothing;
            double peak=0;
            for(int ch=0;ch<channels;ch++) {
                double v=pcm[frame+ch];
                for(Filter f:filters) v=f.sample(v,ch);
                v*=volume;
                if(!Double.isFinite(v)) v=0;
                pcm[frame+ch]=(float)v; peak=Math.max(peak,Math.abs(v));
            }
            double desired=peak>.966?.966/peak:1;
            limiter=desired<limiter?desired:desired+(limiter-desired)*release;
            for(int ch=0;ch<channels;ch++) pcm[frame+ch]=(float)Math.max(-1,Math.min(1,pcm[frame+ch]*limiter));
        }
    }
}
