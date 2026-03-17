package com.zym.hd.course.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CourseSeckillReconcileJob {

    private final CourseSeckillService courseSeckillService;

    public CourseSeckillReconcileJob(CourseSeckillService courseSeckillService) {
        this.courseSeckillService = courseSeckillService;
    }

    @Scheduled(initialDelay = 30000, fixedDelay = 60000)
    public void reconcile() {
        courseSeckillService.reconcileAllSeckillCourses();
    }
}

