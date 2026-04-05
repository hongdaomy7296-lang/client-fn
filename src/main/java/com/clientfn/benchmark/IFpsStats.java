package com.clientfn.benchmark;

public interface IFpsStats {
    double getAverageFps(int windowSeconds);

    double getFrameTimePercentile(double p);
}
