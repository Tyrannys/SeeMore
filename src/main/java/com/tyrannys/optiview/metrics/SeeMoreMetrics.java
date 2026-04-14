package com.tyrannys.optiview.metrics;

import com.tyrannys.optiview.SeeMore;
import com.tyrannys.optiview.metrics.charts.NumberOfWorldsChart;

public class SeeMoreMetrics {
    private final Metrics metrics;

    public SeeMoreMetrics(SeeMore seeMore) {
        this.metrics = new Metrics(seeMore, 30745, seeMore.getSchedulerHook()::runTask);
        addCharts();
    }

    private void addCharts() {
        metrics.addCustomChart(new NumberOfWorldsChart());
    }

}

