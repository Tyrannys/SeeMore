package com.tyrannys.optiview.metrics;

import com.tyrannys.optiview.SeeMore;
import com.tyrannys.optiview.metrics.charts.NumberOfWorldsChart;

public class SeeMoreMetrics {
    private static final int OPTIVIEW_BSTATS_SERVICE_ID = 30745;
    private final Metrics metrics;

    public SeeMoreMetrics(SeeMore seeMore) {
        this.metrics = new Metrics(seeMore, OPTIVIEW_BSTATS_SERVICE_ID, seeMore.getSchedulerHook()::runTask);
        addCharts();
    }

    private void addCharts() {
        metrics.addCustomChart(new NumberOfWorldsChart());
    }

}
